use std::collections::HashMap;
use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicI64, Ordering};
use std::sync::{Arc, Mutex, MutexGuard};
use std::time::Duration;

use boxlite::{
    BoxCommand, BoxInfo, BoxOptions, BoxStatus, BoxliteError, BoxliteOptions, BoxliteResult,
    BoxliteRuntime, CopyOptions, ExecResult, ExecStderr, ExecStdin, ExecStdout, Execution, LiteBox,
    RootfsSpec,
};
use futures::StreamExt;
use jni::JNIEnv;
use jni::objects::{JByteArray, JClass, JObject, JString};
use jni::sys::{jboolean, jint, jlong, jlongArray, jstring};
use once_cell::sync::Lazy;
use serde::de::DeserializeOwned;
use serde::{Deserialize, Serialize};
use tokio::sync::Mutex as AsyncMutex;

const ABI_VERSION: jint = 2;

static NEXT_HANDLE: AtomicI64 = AtomicI64::new(1);
static TOKIO: Lazy<tokio::runtime::Runtime> = Lazy::new(|| {
    tokio::runtime::Builder::new_multi_thread()
        .enable_all()
        .build()
        .expect("Failed to build Tokio runtime for Java JNI bridge")
});
static RUNTIMES: Lazy<Mutex<HashMap<i64, Arc<BoxliteRuntime>>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));
static BOXES: Lazy<Mutex<HashMap<i64, BoxHandleEntry>>> = Lazy::new(|| Mutex::new(HashMap::new()));
static EXECUTIONS: Lazy<Mutex<HashMap<i64, ExecutionHandleEntry>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));

#[derive(Clone)]
struct BoxHandleEntry {
    runtime_handle: i64,
    handle: Arc<LiteBox>,
}

#[derive(Clone)]
struct ExecutionHandleEntry {
    runtime_handle: i64,
    id: String,
    execution: Arc<AsyncMutex<Execution>>,
    stdin: Arc<AsyncMutex<Option<ExecStdin>>>,
    stdout: Arc<AsyncMutex<Option<ExecStdout>>>,
    stderr: Arc<AsyncMutex<Option<ExecStderr>>>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct JavaRuntimeOptions {
    home_dir: Option<String>,
    #[serde(default)]
    image_registries: Vec<String>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct JavaBoxOptions {
    image: Option<String>,
    rootfs_path: Option<String>,
    cpus: Option<u8>,
    memory_mib: Option<u32>,
    disk_size_gb: Option<u64>,
    working_dir: Option<String>,
    #[serde(default)]
    env: HashMap<String, String>,
    auto_remove: Option<bool>,
    detach: Option<bool>,
    entrypoint: Option<Vec<String>>,
    cmd: Option<Vec<String>>,
    user: Option<String>,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct JavaCopyOptions {
    #[serde(default = "default_true")]
    recursive: bool,
    #[serde(default = "default_true")]
    overwrite: bool,
    #[serde(default)]
    follow_symlinks: bool,
    #[serde(default = "default_true")]
    include_parent: bool,
}

#[derive(Debug, Deserialize)]
#[serde(rename_all = "camelCase")]
struct JavaExecCommand {
    command: String,
    #[serde(default)]
    args: Vec<String>,
    #[serde(default)]
    env: HashMap<String, String>,
    timeout_millis: Option<u64>,
    working_dir: Option<String>,
    #[serde(default)]
    tty: bool,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct JavaRuntimeMetrics {
    boxes_created_total: u64,
    boxes_failed_total: u64,
    boxes_stopped_total: u64,
    num_running_boxes: u64,
    total_commands_executed: u64,
    total_exec_errors: u64,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct JavaExecResult {
    exit_code: i32,
    error_message: Option<String>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct JavaBoxStateInfo {
    status: String,
    running: bool,
    pid: Option<u32>,
}

#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
struct JavaBoxInfo {
    id: String,
    name: Option<String>,
    state: JavaBoxStateInfo,
    created_at: String,
    image: String,
    cpus: u8,
    memory_mib: u32,
}

fn default_true() -> bool {
    true
}

fn throw_boxlite_error(env: &mut JNIEnv<'_>, err: BoxliteError) {
    let class = match &err {
        BoxliteError::NotFound(_) => "io/boxlite/NotFoundException",
        BoxliteError::AlreadyExists(_) => "io/boxlite/AlreadyExistsException",
        BoxliteError::InvalidState(_) | BoxliteError::Stopped(_) => {
            "io/boxlite/InvalidStateException"
        }
        BoxliteError::Config(_)
        | BoxliteError::InvalidArgument(_)
        | BoxliteError::Unsupported(_) => "io/boxlite/ConfigException",
        _ => "io/boxlite/InternalException",
    };
    let _ = env.throw_new(class, err.to_string());
}

fn throw_internal(env: &mut JNIEnv<'_>, message: impl AsRef<str>) {
    let _ = env.throw_new("io/boxlite/InternalException", message.as_ref());
}

fn status_to_string(status: BoxStatus) -> String {
    match status {
        BoxStatus::Unknown => "unknown",
        BoxStatus::Configured => "configured",
        BoxStatus::Running => "running",
        BoxStatus::Stopping => "stopping",
        BoxStatus::Stopped => "stopped",
    }
    .to_string()
}

fn box_info_to_java(info: BoxInfo) -> JavaBoxInfo {
    JavaBoxInfo {
        id: info.id.to_string(),
        name: info.name,
        state: JavaBoxStateInfo {
            status: status_to_string(info.status),
            running: info.status.is_running(),
            pid: info.pid,
        },
        created_at: info.created_at.to_rfc3339(),
        image: info.image,
        cpus: info.cpus,
        memory_mib: info.memory_mib,
    }
}

fn runtime_metrics_to_java(metrics: boxlite::RuntimeMetrics) -> JavaRuntimeMetrics {
    JavaRuntimeMetrics {
        boxes_created_total: metrics.boxes_created_total(),
        boxes_failed_total: metrics.boxes_failed_total(),
        boxes_stopped_total: metrics.boxes_stopped_total(),
        num_running_boxes: metrics.num_running_boxes(),
        total_commands_executed: metrics.total_commands_executed(),
        total_exec_errors: metrics.total_exec_errors(),
    }
}

fn allocate_handle() -> i64 {
    NEXT_HANDLE.fetch_add(1, Ordering::Relaxed)
}

fn runtime_handle_from_jlong(handle: jlong) -> BoxliteResult<i64> {
    if handle <= 0 {
        return Err(BoxliteError::InvalidState(format!(
            "runtime handle must be positive, got {handle}"
        )));
    }
    Ok(handle as i64)
}

fn box_handle_from_jlong(handle: jlong) -> BoxliteResult<i64> {
    if handle <= 0 {
        return Err(BoxliteError::InvalidState(format!(
            "box handle must be positive, got {handle}"
        )));
    }
    Ok(handle as i64)
}

fn lock_runtimes() -> BoxliteResult<MutexGuard<'static, HashMap<i64, Arc<BoxliteRuntime>>>> {
    RUNTIMES
        .lock()
        .map_err(|e| BoxliteError::Internal(format!("runtime handle table lock poisoned: {e}")))
}

fn lock_boxes() -> BoxliteResult<MutexGuard<'static, HashMap<i64, BoxHandleEntry>>> {
    BOXES
        .lock()
        .map_err(|e| BoxliteError::Internal(format!("box handle table lock poisoned: {e}")))
}

fn lock_executions() -> BoxliteResult<MutexGuard<'static, HashMap<i64, ExecutionHandleEntry>>> {
    EXECUTIONS
        .lock()
        .map_err(|e| BoxliteError::Internal(format!("execution handle table lock poisoned: {e}")))
}

fn insert_runtime_handle(runtime: BoxliteRuntime) -> BoxliteResult<i64> {
    let native_handle = allocate_handle();
    lock_runtimes()?.insert(native_handle, Arc::new(runtime));
    Ok(native_handle)
}

fn get_runtime(runtime_handle: jlong) -> BoxliteResult<Arc<BoxliteRuntime>> {
    let native_handle = runtime_handle_from_jlong(runtime_handle)?;
    lock_runtimes()?
        .get(&native_handle)
        .cloned()
        .ok_or_else(|| {
            BoxliteError::InvalidState(format!("runtime handle {runtime_handle} is not active"))
        })
}

fn remove_runtime(runtime_handle: jlong) -> BoxliteResult<()> {
    let native_handle = runtime_handle_from_jlong(runtime_handle)?;
    let removed = lock_runtimes()?.remove(&native_handle);
    if removed.is_none() {
        return Err(BoxliteError::InvalidState(format!(
            "runtime handle {runtime_handle} is not active"
        )));
    }

    let mut boxes = lock_boxes()?;
    boxes.retain(|_, entry| entry.runtime_handle != native_handle);

    let mut executions = lock_executions()?;
    executions.retain(|_, entry| entry.runtime_handle != native_handle);
    Ok(())
}

fn insert_box_handle(runtime_handle: i64, handle: LiteBox) -> BoxliteResult<i64> {
    let native_handle = allocate_handle();
    let entry = BoxHandleEntry {
        runtime_handle,
        handle: Arc::new(handle),
    };
    lock_boxes()?.insert(native_handle, entry);
    Ok(native_handle)
}

fn get_box_entry(box_handle: jlong) -> BoxliteResult<BoxHandleEntry> {
    let native_handle = box_handle_from_jlong(box_handle)?;
    let entry = lock_boxes()?.get(&native_handle).cloned().ok_or_else(|| {
        BoxliteError::InvalidState(format!("box handle {box_handle} is not active"))
    })?;

    if !lock_runtimes()?.contains_key(&entry.runtime_handle) {
        return Err(BoxliteError::InvalidState(format!(
            "box handle {box_handle} belongs to a closed runtime"
        )));
    }

    Ok(entry)
}

fn remove_box_handle(box_handle: jlong) -> BoxliteResult<()> {
    let native_handle = box_handle_from_jlong(box_handle)?;
    lock_boxes()?.remove(&native_handle);
    Ok(())
}

fn insert_execution_handle(runtime_handle: i64, mut execution: Execution) -> BoxliteResult<i64> {
    let native_handle = allocate_handle();
    let id = execution.id().to_owned();
    let stdin = execution.stdin();
    let stdout = execution.stdout();
    let stderr = execution.stderr();
    let entry = ExecutionHandleEntry {
        runtime_handle,
        id,
        execution: Arc::new(AsyncMutex::new(execution)),
        stdin: Arc::new(AsyncMutex::new(stdin)),
        stdout: Arc::new(AsyncMutex::new(stdout)),
        stderr: Arc::new(AsyncMutex::new(stderr)),
    };
    lock_executions()?.insert(native_handle, entry);
    Ok(native_handle)
}

fn execution_handle_from_jlong(handle: jlong) -> BoxliteResult<i64> {
    if handle <= 0 {
        return Err(BoxliteError::InvalidState(format!(
            "execution handle must be positive, got {handle}"
        )));
    }
    Ok(handle as i64)
}

fn get_execution_entry(execution_handle: jlong) -> BoxliteResult<ExecutionHandleEntry> {
    let native_handle = execution_handle_from_jlong(execution_handle)?;
    let entry = lock_executions()?
        .get(&native_handle)
        .cloned()
        .ok_or_else(|| {
            BoxliteError::InvalidState(format!("execution handle {execution_handle} is not active"))
        })?;

    if !lock_runtimes()?.contains_key(&entry.runtime_handle) {
        return Err(BoxliteError::InvalidState(format!(
            "execution handle {execution_handle} belongs to a closed runtime"
        )));
    }

    Ok(entry)
}

fn clone_execution(entry: &ExecutionHandleEntry) -> Execution {
    TOKIO.block_on(async {
        let execution = entry.execution.lock().await;
        execution.clone()
    })
}

fn remove_execution_handle(execution_handle: jlong) -> BoxliteResult<()> {
    let native_handle = execution_handle_from_jlong(execution_handle)?;
    lock_executions()?.remove(&native_handle);
    Ok(())
}

fn invalidate_box_handles_for(runtime_handle: i64, id_or_name: &str) -> BoxliteResult<()> {
    let mut boxes = lock_boxes()?;
    boxes.retain(|_, entry| {
        if entry.runtime_handle != runtime_handle {
            return true;
        }

        let id = entry.handle.id().to_string();
        let name = entry.handle.name().map(ToOwned::to_owned);
        !(id == id_or_name || name.as_deref() == Some(id_or_name))
    });
    Ok(())
}

fn read_optional_string(
    env: &mut JNIEnv<'_>,
    value: JString<'_>,
    arg_name: &str,
) -> BoxliteResult<Option<String>> {
    if value.is_null() {
        return Ok(None);
    }

    let text: String = env
        .get_string(&value)
        .map_err(|e| BoxliteError::InvalidArgument(format!("Invalid {arg_name}: {e}")))?
        .into();

    if text.trim().is_empty() {
        return Ok(None);
    }

    Ok(Some(text))
}

fn read_required_string(
    env: &mut JNIEnv<'_>,
    value: JString<'_>,
    arg_name: &str,
) -> BoxliteResult<String> {
    read_optional_string(env, value, arg_name)?.ok_or_else(|| {
        BoxliteError::InvalidArgument(format!("{arg_name} must not be null or blank"))
    })
}

fn read_required_bytes(
    env: &mut JNIEnv<'_>,
    value: JByteArray<'_>,
    arg_name: &str,
) -> BoxliteResult<Vec<u8>> {
    if value.is_null() {
        return Err(BoxliteError::InvalidArgument(format!(
            "{arg_name} must not be null"
        )));
    }
    env.convert_byte_array(&value)
        .map_err(|e| BoxliteError::InvalidArgument(format!("Invalid {arg_name}: {e}")))
}

fn parse_json_from_string<T: DeserializeOwned>(
    env: &mut JNIEnv<'_>,
    value: JString<'_>,
    arg_name: &str,
) -> BoxliteResult<T> {
    let json = read_required_string(env, value, arg_name)?;
    serde_json::from_str(&json)
        .map_err(|e| BoxliteError::Config(format!("Failed to parse {arg_name} JSON: {e}")))
}

fn parse_timeout_seconds(
    env: &mut JNIEnv<'_>,
    timeout_object: JObject<'_>,
) -> BoxliteResult<Option<i32>> {
    if timeout_object.is_null() {
        return Ok(None);
    }

    let value = env
        .call_method(timeout_object, "intValue", "()I", &[])
        .map_err(|e| {
            BoxliteError::InvalidArgument(format!("Invalid timeoutSeconds object: {e}"))
        })?;

    let timeout = value
        .i()
        .map_err(|e| BoxliteError::InvalidArgument(format!("Invalid timeoutSeconds value: {e}")))?;
    Ok(Some(timeout))
}

fn java_runtime_options_to_native(dto: JavaRuntimeOptions) -> BoxliteOptions {
    let mut options = BoxliteOptions::default();
    if let Some(home_dir) = dto.home_dir
        && !home_dir.trim().is_empty()
    {
        options.home_dir = PathBuf::from(home_dir);
    }
    options.image_registries = dto.image_registries;
    options
}

fn java_box_options_to_native(dto: JavaBoxOptions) -> BoxliteResult<BoxOptions> {
    let mut options = BoxOptions::default();
    let image = dto.image.filter(|v| !v.trim().is_empty());
    let rootfs_path = dto.rootfs_path.filter(|v| !v.trim().is_empty());

    if image.is_some() && rootfs_path.is_some() {
        return Err(BoxliteError::Config(
            "BoxOptions requires exactly one of image or rootfsPath".to_string(),
        ));
    }

    if let Some(image) = image {
        options.rootfs = RootfsSpec::Image(image);
    } else if let Some(rootfs_path) = rootfs_path {
        options.rootfs = RootfsSpec::RootfsPath(rootfs_path);
    }

    options.cpus = dto.cpus;
    options.memory_mib = dto.memory_mib;
    options.disk_size_gb = dto.disk_size_gb;
    options.working_dir = dto.working_dir;
    options.env = dto.env.into_iter().collect();
    options.auto_remove = dto.auto_remove.unwrap_or(options.auto_remove);
    options.detach = dto.detach.unwrap_or(options.detach);
    // Java defaults currently serialize empty lists for entrypoint/cmd.
    // Treat empty vectors as "not set" so we don't override image config
    // with an invalid empty entrypoint.
    options.entrypoint = dto.entrypoint.filter(|value| !value.is_empty());
    options.cmd = dto.cmd.filter(|value| !value.is_empty());
    options.user = dto.user;
    options.sanitize()?;
    Ok(options)
}

fn java_copy_options_to_native(dto: JavaCopyOptions) -> CopyOptions {
    CopyOptions {
        recursive: dto.recursive,
        overwrite: dto.overwrite,
        follow_symlinks: dto.follow_symlinks,
        include_parent: dto.include_parent,
    }
}

fn java_exec_command_to_native(dto: JavaExecCommand) -> BoxliteResult<BoxCommand> {
    if dto.command.trim().is_empty() {
        return Err(BoxliteError::InvalidArgument(
            "command must not be null or blank".to_string(),
        ));
    }
    if let Some(timeout_millis) = dto.timeout_millis
        && timeout_millis == 0
    {
        return Err(BoxliteError::InvalidArgument(
            "timeoutMillis must be > 0 when provided".to_string(),
        ));
    }

    let mut command = BoxCommand::new(dto.command).args(dto.args).tty(dto.tty);
    if !dto.env.is_empty() {
        for (key, value) in dto.env {
            command = command.env(key, value);
        }
    }
    if let Some(timeout_millis) = dto.timeout_millis {
        command = command.timeout(Duration::from_millis(timeout_millis));
    }
    if let Some(working_dir) = dto.working_dir
        && !working_dir.trim().is_empty()
    {
        command = command.working_dir(working_dir);
    }
    Ok(command)
}

fn exec_result_to_java(result: ExecResult) -> JavaExecResult {
    JavaExecResult {
        exit_code: result.exit_code,
        error_message: result.error_message,
    }
}

fn serialize_json<T: Serialize>(value: &T) -> BoxliteResult<String> {
    serde_json::to_string(value)
        .map_err(|e| BoxliteError::Internal(format!("Failed to serialize JSON response: {e}")))
}

fn to_jstring(env: &mut JNIEnv<'_>, value: &str) -> jstring {
    match env.new_string(value) {
        Ok(value) => value.into_raw(),
        Err(e) => {
            throw_internal(env, format!("Failed to create Java string: {e}"));
            std::ptr::null_mut()
        }
    }
}

fn to_jlong_array(env: &mut JNIEnv<'_>, values: &[jlong]) -> jlongArray {
    let array = match env.new_long_array(values.len() as i32) {
        Ok(array) => array,
        Err(e) => {
            throw_internal(env, format!("Failed to allocate long[]: {e}"));
            return std::ptr::null_mut();
        }
    };

    if let Err(e) = env.set_long_array_region(&array, 0, values) {
        throw_internal(env, format!("Failed to write long[]: {e}"));
        return std::ptr::null_mut();
    }

    array.into_raw()
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeVersion(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jstring {
    to_jstring(&mut env, env!("CARGO_PKG_VERSION"))
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeNew(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    options_json: JString<'_>,
) -> jlong {
    let result: BoxliteResult<i64> = (|| {
        let dto: JavaRuntimeOptions =
            parse_json_from_string(&mut env, options_json, "optionsJson")?;
        let runtime = BoxliteRuntime::new(java_runtime_options_to_native(dto))?;
        insert_runtime_handle(runtime)
    })();

    match result {
        Ok(handle) => handle as jlong,
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeDefault(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jlong {
    match insert_runtime_handle(BoxliteRuntime::default_runtime().clone()) {
        Ok(handle) => handle as jlong,
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeInitDefault(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    options_json: JString<'_>,
) {
    let result: BoxliteResult<()> = (|| {
        let dto: JavaRuntimeOptions =
            parse_json_from_string(&mut env, options_json, "optionsJson")?;
        BoxliteRuntime::init_default_runtime(java_runtime_options_to_native(dto))
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeFree(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
) {
    if runtime_handle <= 0 {
        return;
    }

    if let Err(err) = remove_runtime(runtime_handle) {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeCreate(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
    box_options_json: JString<'_>,
    name: JString<'_>,
) -> jlong {
    let result: BoxliteResult<i64> = (|| {
        let native_runtime_handle = runtime_handle_from_jlong(runtime_handle)?;
        let runtime = get_runtime(runtime_handle)?;
        let dto: JavaBoxOptions =
            parse_json_from_string(&mut env, box_options_json, "boxOptionsJson")?;
        let options = java_box_options_to_native(dto)?;
        let name = read_optional_string(&mut env, name, "name")?;
        let handle = TOKIO.block_on(runtime.create(options, name))?;
        insert_box_handle(native_runtime_handle, handle)
    })();

    match result {
        Ok(handle) => handle as jlong,
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeGetOrCreate(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
    box_options_json: JString<'_>,
    name: JString<'_>,
) -> jlongArray {
    let result: BoxliteResult<[jlong; 2]> = (|| {
        let native_runtime_handle = runtime_handle_from_jlong(runtime_handle)?;
        let runtime = get_runtime(runtime_handle)?;
        let dto: JavaBoxOptions =
            parse_json_from_string(&mut env, box_options_json, "boxOptionsJson")?;
        let options = java_box_options_to_native(dto)?;
        let name = read_optional_string(&mut env, name, "name")?;
        let (handle, created) = TOKIO.block_on(runtime.get_or_create(options, name))?;
        let box_handle = insert_box_handle(native_runtime_handle, handle)?;
        Ok([box_handle as jlong, if created { 1 } else { 0 }])
    })();

    match result {
        Ok(values) => to_jlong_array(&mut env, &values),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeGet(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
    id_or_name: JString<'_>,
) -> jlong {
    let result: BoxliteResult<jlong> = (|| {
        let native_runtime_handle = runtime_handle_from_jlong(runtime_handle)?;
        let runtime = get_runtime(runtime_handle)?;
        let id_or_name = read_required_string(&mut env, id_or_name, "idOrName")?;
        match TOKIO.block_on(runtime.get(&id_or_name))? {
            Some(handle) => Ok(insert_box_handle(native_runtime_handle, handle)? as jlong),
            None => Ok(0),
        }
    })();

    match result {
        Ok(handle) => handle,
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeGetInfo(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
    id_or_name: JString<'_>,
) -> jstring {
    let result: BoxliteResult<Option<String>> = (|| {
        let runtime = get_runtime(runtime_handle)?;
        let id_or_name = read_required_string(&mut env, id_or_name, "idOrName")?;
        let info = TOKIO.block_on(runtime.get_info(&id_or_name))?;
        match info {
            Some(info) => serialize_json(&box_info_to_java(info)).map(Some),
            None => Ok(None),
        }
    })();

    match result {
        Ok(Some(json)) => to_jstring(&mut env, &json),
        Ok(None) => std::ptr::null_mut(),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeListInfo(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
) -> jstring {
    let result: BoxliteResult<String> = (|| {
        let runtime = get_runtime(runtime_handle)?;
        let infos = TOKIO.block_on(runtime.list_info())?;
        let mapped = infos.into_iter().map(box_info_to_java).collect::<Vec<_>>();
        serialize_json(&mapped)
    })();

    match result {
        Ok(json) => to_jstring(&mut env, &json),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeRemove(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
    id_or_name: JString<'_>,
    force: jboolean,
) {
    let result: BoxliteResult<()> = (|| {
        let native_runtime_handle = runtime_handle_from_jlong(runtime_handle)?;
        let runtime = get_runtime(runtime_handle)?;
        let id_or_name = read_required_string(&mut env, id_or_name, "idOrName")?;
        TOKIO.block_on(runtime.remove(&id_or_name, force != 0))?;
        invalidate_box_handles_for(native_runtime_handle, &id_or_name)
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeMetrics(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
) -> jstring {
    let result: BoxliteResult<String> = (|| {
        let runtime = get_runtime(runtime_handle)?;
        let metrics = TOKIO.block_on(runtime.metrics());
        serialize_json(&runtime_metrics_to_java(metrics))
    })();

    match result {
        Ok(json) => to_jstring(&mut env, &json),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeShutdown(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    runtime_handle: jlong,
    timeout_seconds: JObject<'_>,
) {
    let result: BoxliteResult<()> = (|| {
        let runtime = get_runtime(runtime_handle)?;
        let timeout_seconds = parse_timeout_seconds(&mut env, timeout_seconds)?;
        TOKIO.block_on(runtime.shutdown(timeout_seconds))
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxFree(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
) {
    if box_handle <= 0 {
        return;
    }

    if let Err(err) = remove_box_handle(box_handle) {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxId(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
) -> jstring {
    let result: BoxliteResult<String> = (|| {
        let entry = get_box_entry(box_handle)?;
        Ok(entry.handle.id().to_string())
    })();

    match result {
        Ok(value) => to_jstring(&mut env, &value),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxName(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
) -> jstring {
    let result: BoxliteResult<Option<String>> = (|| {
        let entry = get_box_entry(box_handle)?;
        Ok(entry.handle.name().map(ToOwned::to_owned))
    })();

    match result {
        Ok(Some(value)) => to_jstring(&mut env, &value),
        Ok(None) => std::ptr::null_mut(),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxInfo(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
) -> jstring {
    let result: BoxliteResult<String> = (|| {
        let entry = get_box_entry(box_handle)?;
        let info = entry.handle.info();
        serialize_json(&box_info_to_java(info))
    })();

    match result {
        Ok(json) => to_jstring(&mut env, &json),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxStart(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
) {
    let result: BoxliteResult<()> = (|| {
        let entry = get_box_entry(box_handle)?;
        TOKIO.block_on(entry.handle.start())
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxStop(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
) {
    let result: BoxliteResult<()> = (|| {
        let entry = get_box_entry(box_handle)?;
        TOKIO.block_on(entry.handle.stop())
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxExec(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
    exec_command_json: JString<'_>,
) -> jlong {
    let result: BoxliteResult<i64> = (|| {
        let entry = get_box_entry(box_handle)?;
        let dto: JavaExecCommand =
            parse_json_from_string(&mut env, exec_command_json, "execCommandJson")?;
        let command = java_exec_command_to_native(dto)?;
        let execution = TOKIO.block_on(entry.handle.exec(command))?;
        insert_execution_handle(entry.runtime_handle, execution)
    })();

    match result {
        Ok(handle) => handle as jlong,
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            0
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxCopyIn(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
    host_path: JString<'_>,
    container_dest: JString<'_>,
    copy_options_json: JString<'_>,
) {
    let result: BoxliteResult<()> = (|| {
        let entry = get_box_entry(box_handle)?;
        let host_path = read_required_string(&mut env, host_path, "hostPath")?;
        let container_dest = read_required_string(&mut env, container_dest, "containerDest")?;
        let copy_options: JavaCopyOptions =
            parse_json_from_string(&mut env, copy_options_json, "copyOptionsJson")?;
        TOKIO.block_on(entry.handle.copy_into(
            Path::new(&host_path),
            &container_dest,
            java_copy_options_to_native(copy_options),
        ))
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionFree(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
) {
    if execution_handle <= 0 {
        return;
    }

    if let Err(err) = remove_execution_handle(execution_handle) {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionId(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
) -> jstring {
    let result: BoxliteResult<String> = (|| {
        let entry = get_execution_entry(execution_handle)?;
        Ok(entry.id)
    })();

    match result {
        Ok(value) => to_jstring(&mut env, &value),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionStdinWrite(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
    data: JByteArray<'_>,
) {
    let result: BoxliteResult<()> = (|| {
        let entry = get_execution_entry(execution_handle)?;
        let bytes = read_required_bytes(&mut env, data, "data")?;
        let mut stdin_guard = TOKIO.block_on(entry.stdin.lock());
        let stdin = stdin_guard.as_mut().ok_or_else(|| {
            BoxliteError::InvalidState("stdin is not available for this execution".to_string())
        })?;
        TOKIO.block_on(stdin.write(&bytes))
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionStdinClose(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
) {
    let result: BoxliteResult<()> = (|| {
        let entry = get_execution_entry(execution_handle)?;
        let mut stdin_guard = TOKIO.block_on(entry.stdin.lock());
        let stdin = stdin_guard.as_mut().ok_or_else(|| {
            BoxliteError::InvalidState("stdin is not available for this execution".to_string())
        })?;
        stdin.close();
        Ok(())
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionStdoutNextLine(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
) -> jstring {
    let result: BoxliteResult<Option<String>> = (|| {
        let entry = get_execution_entry(execution_handle)?;
        let mut stdout_guard = TOKIO.block_on(entry.stdout.lock());
        let stdout = stdout_guard.as_mut().ok_or_else(|| {
            BoxliteError::InvalidState("stdout is not available for this execution".to_string())
        })?;
        Ok(TOKIO.block_on(stdout.next()))
    })();

    match result {
        Ok(Some(line)) => to_jstring(&mut env, &line),
        Ok(None) => std::ptr::null_mut(),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionStderrNextLine(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
) -> jstring {
    let result: BoxliteResult<Option<String>> = (|| {
        let entry = get_execution_entry(execution_handle)?;
        let mut stderr_guard = TOKIO.block_on(entry.stderr.lock());
        let stderr = stderr_guard.as_mut().ok_or_else(|| {
            BoxliteError::InvalidState("stderr is not available for this execution".to_string())
        })?;
        Ok(TOKIO.block_on(stderr.next()))
    })();

    match result {
        Ok(Some(line)) => to_jstring(&mut env, &line),
        Ok(None) => std::ptr::null_mut(),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionWait(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
) -> jstring {
    let result: BoxliteResult<String> = (|| {
        let entry = get_execution_entry(execution_handle)?;
        let mut execution = clone_execution(&entry);
        let result = TOKIO.block_on(execution.wait())?;
        serialize_json(&exec_result_to_java(result))
    })();

    match result {
        Ok(json) => to_jstring(&mut env, &json),
        Err(err) => {
            throw_boxlite_error(&mut env, err);
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionKill(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
) {
    let result: BoxliteResult<()> = (|| {
        let entry = get_execution_entry(execution_handle)?;
        let mut execution = clone_execution(&entry);
        TOKIO.block_on(execution.kill())
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeExecutionResizeTty(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    execution_handle: jlong,
    rows: jint,
    cols: jint,
) {
    let result: BoxliteResult<()> = (|| {
        if rows <= 0 || cols <= 0 {
            return Err(BoxliteError::InvalidArgument(
                "rows and cols must both be > 0".to_string(),
            ));
        }
        let entry = get_execution_entry(execution_handle)?;
        let execution = TOKIO.block_on(entry.execution.lock());
        TOKIO.block_on(execution.resize_tty(rows as u32, cols as u32))
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeBoxCopyOut(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    box_handle: jlong,
    container_src: JString<'_>,
    host_dest: JString<'_>,
    copy_options_json: JString<'_>,
) {
    let result: BoxliteResult<()> = (|| {
        let entry = get_box_entry(box_handle)?;
        let container_src = read_required_string(&mut env, container_src, "containerSrc")?;
        let host_dest = read_required_string(&mut env, host_dest, "hostDest")?;
        let copy_options: JavaCopyOptions =
            parse_json_from_string(&mut env, copy_options_json, "copyOptionsJson")?;
        TOKIO.block_on(entry.handle.copy_out(
            &container_src,
            Path::new(&host_dest),
            java_copy_options_to_native(copy_options),
        ))
    })();

    if let Err(err) = result {
        throw_boxlite_error(&mut env, err);
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeAbiVersion(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jint {
    ABI_VERSION
}

#[cfg(test)]
mod tests {
    use super::{
        JavaBoxOptions, JavaExecCommand, java_box_options_to_native, java_exec_command_to_native,
    };
    use boxlite::{BoxliteError, RootfsSpec};
    use std::collections::HashMap;

    #[test]
    fn java_empty_entrypoint_and_cmd_do_not_override_image_defaults() {
        let options = java_box_options_to_native(JavaBoxOptions {
            image: Some("alpine:latest".to_string()),
            rootfs_path: None,
            cpus: None,
            memory_mib: None,
            disk_size_gb: None,
            working_dir: None,
            env: HashMap::new(),
            auto_remove: None,
            detach: None,
            entrypoint: Some(Vec::new()),
            cmd: Some(Vec::new()),
            user: None,
        })
        .expect("conversion should succeed");

        assert!(
            options.entrypoint.is_none(),
            "empty Java entrypoint should map to None"
        );
        assert!(options.cmd.is_none(), "empty Java cmd should map to None");
    }

    #[test]
    fn java_non_empty_entrypoint_and_cmd_are_preserved() {
        let options = java_box_options_to_native(JavaBoxOptions {
            image: Some("alpine:latest".to_string()),
            rootfs_path: None,
            cpus: None,
            memory_mib: None,
            disk_size_gb: None,
            working_dir: None,
            env: HashMap::new(),
            auto_remove: None,
            detach: None,
            entrypoint: Some(vec!["/bin/sh".to_string()]),
            cmd: Some(vec!["-lc".to_string(), "echo hi".to_string()]),
            user: None,
        })
        .expect("conversion should succeed");

        assert_eq!(
            options.entrypoint,
            Some(vec!["/bin/sh".to_string()]),
            "non-empty Java entrypoint should be passed through"
        );
        assert_eq!(
            options.cmd,
            Some(vec!["-lc".to_string(), "echo hi".to_string()]),
            "non-empty Java cmd should be passed through"
        );
        assert!(
            matches!(options.rootfs, RootfsSpec::Image(_)),
            "image input should still map to image rootfs"
        );
    }

    #[test]
    fn java_exec_command_accepts_supported_fields() {
        let mut env = HashMap::new();
        env.insert("FOO".to_string(), "bar".to_string());

        let result = java_exec_command_to_native(JavaExecCommand {
            command: "sh".to_string(),
            args: vec!["-lc".to_string(), "echo hi".to_string()],
            env,
            timeout_millis: Some(1_500),
            working_dir: Some("/workspace".to_string()),
            tty: true,
        });

        assert!(
            result.is_ok(),
            "valid command with args/env/timeout/workingDir/tty should convert successfully"
        );
    }

    #[test]
    fn java_exec_command_rejects_blank_command() {
        let result = java_exec_command_to_native(JavaExecCommand {
            command: "   ".to_string(),
            args: Vec::new(),
            env: HashMap::new(),
            timeout_millis: None,
            working_dir: None,
            tty: false,
        });

        assert!(
            matches!(result, Err(BoxliteError::InvalidArgument(_))),
            "blank command should fail with InvalidArgument"
        );
    }
}
