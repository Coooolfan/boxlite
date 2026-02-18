# BoxLite Rust SDK 能力说明与多 SDK 对比（源码基准）

本文回答 3 个问题：

1. BoxLite 是否提供 Rust SDK？
2. Rust SDK 是否是“功能最全”的 SDK？
3. Rust SDK 具体提供哪些能力？

本文以当前仓库源码为准，不以历史文档为准。

---

## 结论（先看这个）

1. **提供 Rust SDK**。`boxlite` crate 本身就是 Rust SDK，且是核心实现层。
2. **如果“最全”指核心运行时控制能力，Rust SDK 是最全的**。
3. **如果“最全”指高层开箱即用业务封装（如 Browser/Computer 自动化），Python/Node 在高层封装上更丰富**，但底层能力仍来自 Rust。

换句话说：

- 低层控制和能力上限：Rust 最强。
- 业务场景封装速度：Python/Node 更快。

---

## SDK 角色定位

### Rust（核心层）

- 直接实现 Runtime、Box、Exec、Image、Security、Metrics 等核心能力。
- 其它 SDK（Python/Node/Java/C）本质上都是对 Rust 的封装或桥接。

### Python / Node.js（业务友好层）

- 暴露大量常用能力。
- 提供更多高层“现成盒子”封装（如 `CodeBox`、`BrowserBox`、`ComputerBox`、`InteractiveBox`）。
- 但部分底层能力并未全部暴露。

### Java（JVM 生态层）

- 提供 core API（runtime/box/exec/copy）和部分 high-level（`SimpleBox`、`CodeBox`）。
- 当前 `BoxOptions` 字段覆盖范围小于 Rust/Python/Node。

### C（FFI 层）

- 提供 `simple API` 和 `native API`。
- `native API` 通过 JSON 传 `BoxOptions`，灵活度高。
- 但执行模型和接口颗粒度相对更粗。

---

## 功能矩阵（核心能力）

说明：

- `✅`：直接可用
- `⚠️`：部分可用 / 有限制
- `❌`：当前未暴露

| 能力 | Rust | Python | Node.js | Java | C |
|---|---|---|---|---|---|
| Runtime create/get/list/remove/shutdown | ✅ | ✅ | ✅ | ✅ | ✅ |
| `get_or_create` | ✅ | ✅ | ✅ | ✅ | ❌ |
| `exists` | ✅ | ❌ | ❌ | ❌ | ❌ |
| 镜像 API：`pull_image` / `list_images` | ✅ | ❌ | ❌ | ❌ | ❌ |
| 本地 OCI layout (`rootfsPath`) | ✅ | ✅ | ✅ | ✅ | ✅（JSON） |
| Box start/stop/info/metrics | ✅ | ✅ | ✅ | ✅ | ✅ |
| copy in/out | ✅ | ✅ | ✅ | ✅ | ❌ |
| exec 流式 stdin/stdout/stderr | ✅ | ✅ | ✅ | ✅ | ⚠️（回调/缓冲，非独立句柄） |
| exec `kill` | ✅ | ✅ | ✅ | ✅ | ⚠️（无独立 execution kill API） |
| exec `signal`（任意信号） | ✅ | ❌ | ❌ | ❌ | ❌ |
| exec `resize_tty` | ✅ | ❌ | ❌ | ✅ | ❌ |
| BoxOptions: volumes/ports/network | ✅ | ✅ | ✅ | ❌ | ✅（JSON） |
| BoxOptions: security 细粒度配置 | ✅ | ⚠️（部分字段） | ❌（默认） | ❌ | ✅（JSON） |

要点：

- Rust 独有且非常关键的能力：`pull_image`、`list_images`、`exists`、`Execution::signal`。
- Java 当前 `BoxOptions` 不含 `volumes/ports/network/security`。
- Node 当前 `BoxOptions` 没有 security 自定义入口（固定默认）。
- Python 有 security 配置，但不是 Rust 全量字段（例如 `uid/gid/chroot/env_allowlist` 等不在 Python 暴露面）。

---

## Rust SDK 详细能力清单

## 1. Runtime 能力（`BoxliteRuntime`）

常用入口：

- `BoxliteRuntime::new(options)`
- `BoxliteRuntime::with_defaults()`
- `BoxliteRuntime::default_runtime()`
- `BoxliteRuntime::init_default_runtime(options)`

运行时管理：

- `create(options, name)`
- `get_or_create(options, name)`
- `get(id_or_name)`
- `get_info(id_or_name)`
- `list_info()`
- `exists(id_or_name)`
- `remove(id_or_name, force)`
- `metrics()`
- `shutdown(timeout)`

镜像管理（Rust 侧直接提供）：

- `pull_image(image_ref)`
- `list_images()`

这两项是其它 SDK 目前未直接暴露的重要差异。

## 2. Box 配置能力（`BoxOptions`）

Rust `BoxOptions` 覆盖非常完整：

- 资源：`cpus`、`memory_mib`、`disk_size_gb`
- 文件系统：`rootfs`、`volumes`、`working_dir`
- 网络：`network`、`ports`
- 运行行为：`auto_remove`、`detach`
- 安全：`security`
- 启动命令：`entrypoint`、`cmd`、`user`
- 挂载隔离：`isolate_mounts`（Linux）

`rootfs` 支持两种来源：

- `RootfsSpec::Image("alpine:latest")`
- `RootfsSpec::RootfsPath("/path/to/oci-layout")`

其中 `RootfsPath` 走本地 OCI bundle 加载，要求目录包含：

- `oci-layout`
- `index.json`
- `blobs/sha256/...`

## 3. 安全配置能力（`SecurityOptions` + Builder）

Rust 支持完整安全配置：

- 预设：`development()` / `standard()` / `maximum()`
- jailer/seccomp
- uid/gid 降权
- namespace 开关
- chroot 基目录与开关
- FD 与环境变量清洗
- 资源限制（open files/process/memory/cpu time 等）
- macOS sandbox profile 与网络策略

并提供 `SecurityOptionsBuilder` 做细粒度链式配置。

## 4. Box 操作能力（`LiteBox`）

- `id()` / `name()` / `info()`
- `start()` / `stop()`
- `exec(BoxCommand)`
- `metrics()`
- `copy_into(...)` / `copy_out(...)`

`exec()` 会在需要时隐式启动 box。

## 5. 执行控制能力（`BoxCommand` + `Execution`）

`BoxCommand` 支持：

- `args(...)`
- `env(...)`
- `timeout(...)`
- `working_dir(...)`
- `tty(true/false)`

`Execution` 支持：

- `stdin()` / `stdout()` / `stderr()`（流式）
- `wait()`
- `kill()`
- `signal(i32)`（Rust 独有，通用信号控制）
- `resize_tty(rows, cols)`

## 6. 指标能力（`RuntimeMetrics` / `BoxMetrics`）

运行时指标：

- `boxes_created_total`
- `boxes_failed_total`
- `boxes_stopped_total`
- `num_running_boxes`
- `total_commands_executed`
- `total_exec_errors`

Box 指标：

- `cpu_percent`、`memory_bytes`
- 网络发送/接收字节、TCP 连接与错误
- 执行计数与错误计数
- 分阶段启动耗时（filesystem/image/guest_rootfs/spawn/init）

---

## Rust 使用示例（可作为基线模板）

```rust
use boxlite::{
    BoxCommand, BoxOptions, BoxliteRuntime, CopyOptions, RootfsSpec, SecurityOptions,
};
use futures::StreamExt;

#[tokio::main]
async fn main() -> Result<(), Box<dyn std::error::Error>> {
    // 1) Runtime
    let runtime = BoxliteRuntime::with_defaults()?;

    // 2) 可选：预拉镜像 + 查看本地缓存镜像
    runtime.pull_image("alpine:latest").await?;
    let images = runtime.list_images().await?;
    println!("cached images: {}", images.len());

    // 3) 创建 Box
    let options = BoxOptions {
        rootfs: RootfsSpec::Image("alpine:latest".into()),
        cpus: Some(1),
        memory_mib: Some(256),
        auto_remove: true,
        security: SecurityOptions::standard(),
        ..Default::default()
    };
    let bx = runtime.create(options, Some("rust-sdk-demo".to_string())).await?;

    // 4) 执行命令并流式读取输出
    let mut exec = bx
        .exec(BoxCommand::new("sh").args(["-lc", "echo hello && uname -a"]))
        .await?;

    if let Some(mut stdout) = exec.stdout() {
        while let Some(line) = stdout.next().await {
            println!("[stdout] {line}");
        }
    }

    let result = exec.wait().await?;
    println!("exit_code={}", result.code());

    // 5) copy in/out
    bx.copy_into("Cargo.toml", "/tmp/Cargo.toml", CopyOptions::default())
        .await?;
    bx.copy_out("/tmp/Cargo.toml", "/tmp/boxlite-out", CopyOptions::default())
        .await?;

    // 6) 关闭 runtime（会优雅停止）
    runtime.shutdown(None).await?;
    Ok(())
}
```

本地 OCI layout 示例（不走 registry）：

```rust
let options = BoxOptions {
    rootfs: RootfsSpec::RootfsPath("/path/to/oci-layout-dir".into()),
    ..Default::default()
};
let bx = runtime.create(options, None).await?;
```

---

## 是否“最全”的结论建议

如果你的需求是以下任一项，建议优先 Rust：

- 需要完整核心能力和最细粒度控制
- 需要镜像预拉取/缓存查询 API（`pull_image`/`list_images`）
- 需要执行级信号控制（`Execution::signal`）
- 需要完整安全隔离参数（而不是 SDK 裁剪后的子集）

如果你更关注“业务场景开箱即用”，建议：

- Python/Node：更丰富的高层封装（Code/Browser/Computer/Interactive 等）
- Java：JVM 集成友好，核心能力可用，但配置面当前较窄
- C：跨语言嵌入友好，Native API 灵活，但接口颗粒度较粗

---

## 源码依据（关键路径）

Rust 核心：

- `boxlite/src/runtime/core.rs`
- `boxlite/src/runtime/options.rs`
- `boxlite/src/litebox/mod.rs`
- `boxlite/src/litebox/exec.rs`
- `boxlite/src/litebox/copy.rs`
- `boxlite/src/images/store.rs`

Python：

- `sdks/python/src/runtime.rs`
- `sdks/python/src/options.rs`
- `sdks/python/src/box_handle.rs`
- `sdks/python/src/exec.rs`

Node.js：

- `sdks/node/src/runtime.rs`
- `sdks/node/src/options.rs`
- `sdks/node/src/box_handle.rs`
- `sdks/node/src/exec.rs`

Java：

- `sdks/java/sdk-core/src/main/java/io/boxlite/BoxliteRuntime.java`
- `sdks/java/sdk-core/src/main/java/io/boxlite/BoxOptions.java`
- `sdks/java/sdk-core/src/main/java/io/boxlite/BoxHandle.java`
- `sdks/java/sdk-core/src/main/java/io/boxlite/ExecutionHandle.java`

C：

- `sdks/c/include/boxlite.h`
- `sdks/c/src/ffi.rs`

