# Java SDK 技术路线图 (JVM 25, 直连 Rust)

## 1. 目标与范围

### 1.1 目标

为 BoxLite 新增 Java SDK，基于 **JVM 25**，并且 **Java 直接通过 JNI 调用 Rust 核心 (`boxlite` crate)**，不经过现有 C API 包装层。  
能力目标对标当前 Python SDK（核心绑定 + 高层封装）。

### 1.2 成功标准

- Java 核心 API 覆盖 Python SDK 核心能力：
  - Runtime 生命周期：`create/get/get_or_create/get_info/list_info/remove/shutdown/metrics`
  - Box 生命周期：`start/stop/info/metrics/copy_in/copy_out`
  - Execution：`stdin/stdout/stderr/wait/kill/resize_tty`
  - 配置对象：`Options/BoxOptions/SecurityOptions/CopyOptions`
  - 指标对象：`RuntimeMetrics/BoxMetrics` 全字段映射
- 高层封装覆盖 Python 常用能力：
  - `SimpleBox`、`CodeBox`、`BrowserBox`、`ComputerBox`、`InteractiveBox`
  - `SkillBox` 作为后续增强交付
- 发布形态可被 Java 与 Kotlin 项目直接消费（Maven Central）。

### 1.3 非目标

- 不在 v1 做“绕过虚拟化能力限制”的模拟实现（仍依赖 KVM/HVF）。
- 不在 v1 引入第二套 Go/Java 共用中间进程协议。
- 不在 v1 改写 Rust runtime 核心架构（仅增量扩展绑定层与必要桥接）。

## 2. 当前能力基线（Rust 与 Python）

Rust 核心已具备 Java 所需主要能力：

- Runtime API：`boxlite/src/runtime/core.rs`
- Box API：`boxlite/src/litebox/mod.rs`
- Execution + 流：`boxlite/src/litebox/exec.rs`
- Options/Security：`boxlite/src/runtime/options.rs`
- Metrics：`boxlite/src/metrics/runtime_metrics.rs`、`boxlite/src/metrics/box_metrics.rs`
- Error 模型：`boxlite-shared/src/errors.rs`

Python SDK 已证明这套能力可完整封装：

- 核心绑定：`sdks/python/src/`
- 高层封装：`sdks/python/boxlite/`

结论：Java SDK 的关键是“设计一套稳定的 JNI 边界 + Java 友好 API + 工程化交付链路”。

## 3. 总体技术方案

### 3.1 架构分层

建议新增 `sdks/java/` 多模块结构：

- `sdk-core`（纯 Java）
  - 对外 API、领域模型、异常体系、文档与示例
- `sdk-native-loader`（纯 Java）
  - native 库加载、平台识别、版本校验
- `sdk-highlevel`（纯 Java）
  - `SimpleBox/CodeBox/...` 等高层封装
- `sdk-kotlin-ext`（可选）
  - Kotlin 扩展（suspend/DSL）
- `native`（Rust crate）
  - `jni-rs` + `boxlite`，实现 JNI 桥接

### 3.2 绑定策略（直连 Rust）

- Java ↔ Rust 使用 JNI。
- Rust `native` crate 直接依赖 `boxlite`。
- 不依赖 `sdks/c/include/boxlite.h`，不复用 C ABI 数据结构。
- 所有复杂对象通过 “native handle + Java wrapper” 管理。

### 3.3 并发与异步策略（JVM 25）

- Java 对外默认返回 `CompletableFuture<T>`。
- 阻塞友好 API 由 Java 层包装（`future.join()` 或显式 `Blocking` 方法）。
- JVM 25 下推荐虚拟线程执行 native 阻塞等待，降低线程占用成本。
- Rust 侧统一使用 Tokio runtime，避免每次调用临时创建 runtime。

## 4. API 对标清单（对齐 Python SDK）

### 4.1 Runtime

- `BoxliteRuntime.new(options)`
- `BoxliteRuntime.defaultRuntime()`
- `initDefaultRuntime(options)`
- `create(options, name)`
- `getOrCreate(options, name)` -> `(BoxHandle, created)`
- `get(idOrName)`
- `getInfo(idOrName)`
- `listInfo()`
- `remove(idOrName, force)`
- `metrics()`
- `shutdown(timeoutSeconds)`

### 4.2 Box

- `id`, `name`, `info()`
- `start()`, `stop()`
- `exec(commandBuilder)`
- `metrics()`
- `copyIn(hostPath, containerDest, copyOptions)`
- `copyOut(containerSrc, hostDest, copyOptions)`

### 4.3 Execution

- `id()`
- `stdin().write(bytes) / close()`
- `stdout().nextLine()`（或 `Flow.Publisher<String>`）
- `stderr().nextLine()`
- `wait() -> ExecResult(exitCode, errorMessage)`
- `kill()`
- `resizeTty(rows, cols)`

### 4.4 配置与指标

- `Options`, `BoxOptions`, `SecurityOptions`, `CopyOptions`
- `RuntimeMetrics`（创建数、失败数、运行数、命令总数、错误总数）
- `BoxMetrics`（命令、I/O 字节、CPU/内存、网络、阶段耗时）

### 4.5 高层封装（Java 语义）

- `SimpleBox`：通用命令执行
- `CodeBox`：代码执行便捷 API
- `BrowserBox`：浏览器自动化封装
- `ComputerBox`：桌面自动化封装
- `InteractiveBox`：交互终端/TTY
- `SkillBox`：后续增强（依赖外部工具链与镜像流程）

## 5. JNI 设计细节

### 5.1 Handle 设计

- Java 侧对象仅存 `long nativeHandle`。
- Rust 侧维护类型化句柄表（Runtime/Box/Execution/Stream 分离）。
- 提供统一 `close/free`，并用 `Cleaner` 做兜底回收。
- 明确“已释放句柄”错误，防止 UAF（use-after-free）。

### 5.2 错误映射

- Rust `BoxliteError` 映射到 Java `BoxliteException` 子类：
  - `NotFoundException`
  - `AlreadyExistsException`
  - `InvalidStateException`
  - `ExecutionException`
  - `ConfigException`
  - `NetworkException`
  - `InternalException`
- 保留 `code` + `message` + `cause`（若有）。

### 5.3 字符串与二进制

- 文本统一 UTF-8。
- `stdin`/`copy` 等场景优先走 byte[]/ByteBuffer，避免不必要编码转换。

### 5.4 流与背压

- 方案 A（v1 推荐）：拉模型（`nextLine()`），简单可靠。
- 方案 B（v1.1 可选）：推模型（`Flow.Publisher<String>`）提升响应性。
- 对超大输出增加限流/取消策略，防止 Java 侧消费不足导致积压。

## 6. 分阶段实施计划

### Phase 0：工程骨架与最小可运行

交付：

- `sdks/java/` 目录和 Gradle 多模块骨架
- `sdks/java/native` Rust crate（`jni-rs`）
- native 加载器（macOS arm64、linux x86_64/aarch64）
- 最小 smoke test：`version()` / `runtime.new()`

验收：

- CI 可构建 jar + native 动态库
- 示例程序可本地加载并调用最小 API

### Phase 1：Runtime + Box 核心能力

交付：

- Runtime 全套管理 API
- Box 生命周期与 `copyIn/copyOut`
- `BoxOptions/Options` 首版映射

验收：

- 对标 Python 核心场景：创建、重连、删除、关闭
- 集成测试覆盖 name/id 两种寻址路径

### Phase 2：Execution 全能力

交付：

- `exec` 返回 `Execution`
- stdin/stdout/stderr 流、`wait/kill/resizeTty`
- `env/tty/working_dir/timeout` 映射

验收：

- 大输出、长命令、交互命令、异常终止全通过
- 无死锁、无句柄泄漏

### Phase 3：配置与指标对齐 Python

交付：

- `SecurityOptions`、`CopyOptions` 全字段映射
- `RuntimeMetrics`、`BoxMetrics` 全字段映射
- 错误体系稳定化

验收：

- 字段级契约测试（和 Python 字段语义一致）
- 指标对象序列化/日志输出可读

### Phase 4：高层封装

交付：

- `SimpleBox`、`CodeBox`、`InteractiveBox`
- `BrowserBox`、`ComputerBox`
- `SkillBox`（可拆到 4.5）

验收：

- Java 示例覆盖 Python README 主路径
- API 用法具备 Java 习惯（builder、try-with-resources）

### Phase 5：发布与长期维护

交付：

- Maven Central 发布流程
- 平台制品（classifier）与版本矩阵
- 兼容性策略与升级指南

验收：

- 每次发布自动产出 JavaDoc + 示例
- 支持矩阵与问题排查文档完善

## 7. CI 与测试策略

### 7.1 测试分层

- Rust native 单测：句柄管理、错误映射、字符串转换
- Java 单测：API 行为、参数校验、异常断言
- 集成测试：真实 VM 场景（create/exec/copy/metrics）
- E2E 示例测试：对外 README 示例可运行

## 7. 风险与缓解

- JNI 生命周期复杂：  
  缓解：统一句柄表 + `Cleaner` + 强制 close 契约 + 泄漏检测测试。

- 流式 I/O 背压问题：  
  缓解：v1 先用拉模型，提供明确超时/取消机制。

- 跨平台打包复杂：  
  缓解：先聚焦主平台（macOS arm64 + Linux x86_64/aarch64），再扩展。

- 高层封装范围膨胀：  
  缓解：分期交付，先保核心对齐，再补特性封装。

## 8. Rust 侧改动边界

需要改 Rust，但以“新增绑定层”为主：

- 新增 `sdks/java/native` crate（必做）
- 视需要为 JNI 增加少量桥接辅助函数（可选）
- 不改 Runtime 核心语义和所有权边界（原则）

## 9. 里程碑定义（建议）

- M1：可创建 runtime + box（Phase 0/1）
- M2：可流式执行命令（Phase 2）
- M3：配置/指标全量对齐（Phase 3）
- M4：高层封装基本完备（Phase 4）
- M5：可发布并持续维护（Phase 5）

---

优先保证“可交付”和“可维护”：先把 JNI 核心面打稳，再做高层封装扩展。对标 Python SDK 的目标是 **能力等价**，不是逐行复制实现细节
