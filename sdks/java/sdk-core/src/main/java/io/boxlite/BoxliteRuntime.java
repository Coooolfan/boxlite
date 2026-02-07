package io.boxlite;

import io.boxlite.loader.NativeBindings;
import java.lang.ref.Cleaner;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

/** 用于创建和管理 BoxLite 盒子的运行时句柄。 */
public final class BoxliteRuntime implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();
    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final RuntimeState state;
    private final Cleaner.Cleanable cleanable;

    private BoxliteRuntime(long nativeHandle) {
        this.state = new RuntimeState(nativeHandle);
        this.cleanable = CLEANER.register(this, state);
    }

    /**
     * 使用默认选项创建独立运行时。
     *
     * @return 新的运行时句柄。
     */
    public static BoxliteRuntime create() {
        return create(Options.defaults());
    }

    /**
     * 使用自定义选项创建独立运行时。
     *
     * @param options 运行时配置，传 {@code null} 等价于 {@link Options#defaults()}。
     * @return 新的运行时句柄。
     */
    public static BoxliteRuntime create(Options options) {
        Options resolvedOptions = options == null ? Options.defaults() : options;
        long handle = NativeBindings.runtimeNew(JsonSupport.write(toRuntimePayload(resolvedOptions)));
        return fromNativeHandle(handle, "runtimeNew");
    }

    /**
     * 返回进程级默认运行时句柄。
     *
     * @return 默认运行时句柄。
     */
    public static BoxliteRuntime defaultRuntime() {
        long handle = NativeBindings.runtimeDefault();
        return fromNativeHandle(handle, "runtimeDefault");
    }

    /**
     * 初始化进程级默认运行时。
     *
     * <p>如需覆盖默认值，请在首次调用 {@link #defaultRuntime()} 前调用。
     *
     * @param options 运行时配置，传 {@code null} 等价于 {@link Options#defaults()}。
     */
    public static void initDefaultRuntime(Options options) {
        Options resolvedOptions = options == null ? Options.defaults() : options;
        NativeBindings.runtimeInitDefault(JsonSupport.write(toRuntimePayload(resolvedOptions)));
    }

    /**
     * 创建盒子。
     *
     * @param options 盒子选项，传 {@code null} 等价于 {@link BoxOptions#defaults()}。
     * @param name 盒子名称，可为 {@code null}。
     * @return 异步返回已创建的盒子句柄。
     */
    public CompletableFuture<BoxHandle> create(BoxOptions options, String name) {
        BoxOptions resolvedOptions = options == null ? BoxOptions.defaults() : options;
        return async(() -> {
            long runtimeHandle = requireNativeHandle();
            long boxHandle = NativeBindings.runtimeCreate(
                runtimeHandle,
                JsonSupport.write(resolvedOptions),
                name
            );
            return new BoxHandle(this, requireValidNativeHandle(boxHandle, "runtimeCreate"));
        });
    }

    /**
     * 创建未显式命名的盒子。
     *
     * @param options 盒子选项，传 {@code null} 等价于 {@link BoxOptions#defaults()}。
     * @return 异步返回已创建的盒子句柄。
     */
    public CompletableFuture<BoxHandle> create(BoxOptions options) {
        return create(options, null);
    }

    /**
     * 按名称查找盒子，不存在时创建。
     *
     * @param options 需要创建时使用的盒子选项。
     * @param name 用于查找的盒子名称，可为 {@code null}。
     * @return 异步返回盒子句柄及是否新建标记。
     */
    public CompletableFuture<GetOrCreateResult> getOrCreate(BoxOptions options, String name) {
        BoxOptions resolvedOptions = options == null ? BoxOptions.defaults() : options;
        return async(() -> {
            long runtimeHandle = requireNativeHandle();
            long[] nativeResult = NativeBindings.runtimeGetOrCreate(
                runtimeHandle,
                JsonSupport.write(resolvedOptions),
                name
            );

            if (nativeResult == null || nativeResult.length != 2) {
                throw new InternalException("native runtimeGetOrCreate returned invalid result shape");
            }

            long boxHandle = requireValidNativeHandle(nativeResult[0], "runtimeGetOrCreate");
            boolean created = nativeResult[1] != 0L;
            return new GetOrCreateResult(new BoxHandle(this, boxHandle), created);
        });
    }

    /**
     * 获取或创建未命名盒子。
     *
     * @param options 需要创建时使用的盒子选项。
     * @return 异步返回盒子句柄及是否新建标记。
     */
    public CompletableFuture<GetOrCreateResult> getOrCreate(BoxOptions options) {
        return getOrCreate(options, null);
    }

    /**
     * 通过 ID 或名称获取盒子句柄。
     *
     * @param idOrName 盒子 ID 或盒子名称。
     * @return 异步返回盒子句柄；未找到时为空。
     */
    public CompletableFuture<Optional<BoxHandle>> get(String idOrName) {
        requireIdOrName(idOrName);
        return async(() -> {
            long boxHandle = NativeBindings.runtimeGet(requireNativeHandle(), idOrName);
            if (boxHandle == 0L) {
                return Optional.empty();
            }
            return Optional.of(new BoxHandle(this, boxHandle));
        });
    }

    /**
     * 通过 ID 或名称获取盒子元数据。
     *
     * @param idOrName 盒子 ID 或盒子名称。
     * @return 异步返回盒子元数据；未找到时为空。
     */
    public CompletableFuture<Optional<BoxInfo>> getInfo(String idOrName) {
        requireIdOrName(idOrName);
        return async(() -> {
            String json = NativeBindings.runtimeGetInfo(requireNativeHandle(), idOrName);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(JsonSupport.read(json, BoxInfo.class));
        });
    }

    /**
     * 列出所有已知盒子的元数据。
     *
     * @return 异步返回盒子元数据列表。
     */
    public CompletableFuture<List<BoxInfo>> listInfo() {
        return async(() -> {
            String json = NativeBindings.runtimeListInfo(requireNativeHandle());
            return JsonSupport.readList(json, BoxInfo.class);
        });
    }

    /**
     * 通过 ID 或名称删除盒子。
     *
     * @param idOrName 盒子 ID 或盒子名称。
     * @param force 是否强制删除运行中的盒子。
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> remove(String idOrName, boolean force) {
        requireIdOrName(idOrName);
        return async(() -> {
            NativeBindings.runtimeRemove(requireNativeHandle(), idOrName, force);
            return null;
        });
    }

    /**
     * 通过 ID 或名称删除盒子（不强制）。
     *
     * @param idOrName 盒子 ID 或盒子名称。
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> remove(String idOrName) {
        return remove(idOrName, false);
    }

    /**
     * 读取运行时指标。
     *
     * @return 异步返回指标快照。
     */
    public CompletableFuture<RuntimeMetrics> metrics() {
        return async(() -> {
            String json = NativeBindings.runtimeMetrics(requireNativeHandle());
            return JsonSupport.read(json, RuntimeMetrics.class);
        });
    }

    /**
     * 关闭运行时。
     *
     * @param timeoutSeconds 可选优雅关闭超时（秒），传 {@code null} 使用默认值。
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> shutdown(Integer timeoutSeconds) {
        return async(() -> {
            NativeBindings.runtimeShutdown(requireNativeHandle(), timeoutSeconds);
            return null;
        });
    }

    /** 释放原生运行时句柄，可重复调用。 */
    @Override
    public void close() {
        cleanable.clean();
    }

    long requireNativeHandle() {
        return state.requireNativeHandle();
    }

    <T> CompletableFuture<T> async(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, EXECUTOR);
    }

    private static BoxliteRuntime fromNativeHandle(long handle, String operation) {
        return new BoxliteRuntime(requireValidNativeHandle(handle, operation));
    }

    private static long requireValidNativeHandle(long handle, String operation) {
        if (handle == 0L) {
            throw new InternalException("Native " + operation + " returned invalid handle 0");
        }
        return handle;
    }

    private static void requireIdOrName(String idOrName) {
        if (idOrName == null || idOrName.isBlank()) {
            throw new ConfigException("idOrName must not be null or blank");
        }
    }

    private static RuntimePayload toRuntimePayload(Options options) {
        Path homeDir = options.homeDir();
        String normalizedHomeDir = null;
        if (homeDir != null) {
            normalizedHomeDir = homeDir.toAbsolutePath().normalize().toString();
        }
        return new RuntimePayload(normalizedHomeDir, options.imageRegistries());
    }

    private record RuntimePayload(String homeDir, List<String> imageRegistries) {
    }

    private static final class RuntimeState implements Runnable {
        private final AtomicLong handle;

        private RuntimeState(long handle) {
            this.handle = new AtomicLong(handle);
        }

        private long requireNativeHandle() {
            long value = handle.get();
            if (value == 0L) {
                throw new InvalidStateException("Runtime handle is closed");
            }
            return value;
        }

        @Override
        public void run() {
            long value = handle.getAndSet(0L);
            if (value == 0L) {
                return;
            }

            try {
                NativeBindings.runtimeFree(value);
            } catch (RuntimeException ignored) {
                // Cleaner fallback must not fail on GC threads.
            }
        }
    }
}
