use std::collections::HashMap;
use std::sync::Mutex;
use std::sync::atomic::{AtomicI64, Ordering};

use boxlite::runtime::BoxliteRuntime;
use jni::JNIEnv;
use jni::objects::JClass;
use jni::sys::{jint, jlong, jstring};
use once_cell::sync::Lazy;

const ABI_VERSION: jint = 1;

static NEXT_HANDLE: AtomicI64 = AtomicI64::new(1);
static RUNTIMES: Lazy<Mutex<HashMap<i64, BoxliteRuntime>>> = Lazy::new(|| Mutex::new(HashMap::new()));

fn throw_exception(env: &mut JNIEnv<'_>, message: impl AsRef<str>) {
    let _ = env.throw_new("io/boxlite/BoxliteException", message.as_ref());
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeVersion(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jstring {
    match env.new_string(env!("CARGO_PKG_VERSION")) {
        Ok(version) => version.into_raw(),
        Err(err) => {
            throw_exception(&mut env, format!("Failed to create Java string: {err}"));
            std::ptr::null_mut()
        }
    }
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeNew(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jlong {
    let runtime = match BoxliteRuntime::with_defaults() {
        Ok(runtime) => runtime,
        Err(err) => {
            throw_exception(&mut env, format!("Failed to create BoxliteRuntime: {err}"));
            return 0;
        }
    };

    let handle = NEXT_HANDLE.fetch_add(1, Ordering::Relaxed);

    let mut map = match RUNTIMES.lock() {
        Ok(guard) => guard,
        Err(err) => {
            throw_exception(&mut env, format!("Runtime handle table lock poisoned: {err}"));
            return 0;
        }
    };

    map.insert(handle, runtime);
    handle as jlong
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeRuntimeFree(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    handle: jlong,
) {
    if handle <= 0 {
        return;
    }

    let mut map = match RUNTIMES.lock() {
        Ok(guard) => guard,
        Err(err) => {
            throw_exception(&mut env, format!("Runtime handle table lock poisoned: {err}"));
            return;
        }
    };

    map.remove(&(handle as i64));
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_io_boxlite_loader_NativeBindings_nativeAbiVersion(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jint {
    ABI_VERSION
}
