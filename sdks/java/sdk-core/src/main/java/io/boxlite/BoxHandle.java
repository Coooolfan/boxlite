package io.boxlite;

import io.boxlite.loader.NativeBindings;
import java.lang.ref.Cleaner;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** 原生 BoxLite 盒子句柄。 */
public final class BoxHandle implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();

    private final BoxliteRuntime runtime;
    private final BoxState state;
    private final String id;
    private final Optional<String> name;
    private final Cleaner.Cleanable cleanable;

    BoxHandle(BoxliteRuntime runtime, long nativeHandle) {
        this.runtime = runtime;
        this.state = new BoxState(nativeHandle);
        this.id = NativeBindings.boxId(nativeHandle);
        this.name = Optional.ofNullable(NativeBindings.boxName(nativeHandle));
        this.cleanable = CLEANER.register(this, state);
    }

    /**
     * 返回不可变的盒子 ID。
     *
     * @return 盒子 ID。
     */
    public String id() {
        return id;
    }

    /**
     * 返回可选的盒子名称。
     *
     * @return 名称存在时返回对应值。
     */
    public Optional<String> name() {
        return name;
    }

    /**
     * 获取当前盒子元数据。
     *
     * @return 当前盒子信息。
     */
    public BoxInfo info() {
        runtime.requireNativeHandle();
        String json = NativeBindings.boxInfo(state.requireNativeHandle());
        return JsonSupport.read(json, BoxInfo.class);
    }

    /**
     * 启动盒子。
     *
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> start() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            NativeBindings.boxStart(state.requireNativeHandle());
            return null;
        });
    }

    /**
     * 停止盒子。
     *
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> stop() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            NativeBindings.boxStop(state.requireNativeHandle());
            return null;
        });
    }

    /**
     * 在盒子内执行命令。
     *
     * @param command 执行选项。
     * @return 异步返回执行句柄。
     */
    public CompletableFuture<ExecutionHandle> exec(ExecCommand command) {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            if (command == null) {
                throw new ConfigException("command must not be null");
            }

            long execHandle = NativeBindings.boxExec(
                state.requireNativeHandle(),
                JsonSupport.write(command)
            );
            if (execHandle == 0L) {
                throw new InternalException("Native boxExec returned invalid handle 0");
            }
            return new ExecutionHandle(runtime, execHandle);
        });
    }

    /**
     * 将宿主机内容复制到盒子内。
     *
     * @param hostPath 宿主机源路径。
     * @param containerDest 盒子内目标路径。
     * @param options 复制选项，传 {@code null} 等价于 {@link CopyOptions#defaults()}。
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> copyIn(Path hostPath, String containerDest, CopyOptions options) {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            if (hostPath == null) {
                throw new ConfigException("hostPath must not be null");
            }
            if (containerDest == null || containerDest.isBlank()) {
                throw new ConfigException("containerDest must not be null or blank");
            }

            CopyOptions resolvedOptions = options == null ? CopyOptions.defaults() : options;
            NativeBindings.boxCopyIn(
                state.requireNativeHandle(),
                hostPath.toString(),
                containerDest,
                JsonSupport.write(resolvedOptions)
            );
            return null;
        });
    }

    /**
     * 将盒子内内容复制到宿主机。
     *
     * @param containerSrc 盒子内源路径。
     * @param hostDest 宿主机目标路径。
     * @param options 复制选项，传 {@code null} 等价于 {@link CopyOptions#defaults()}。
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> copyOut(String containerSrc, Path hostDest, CopyOptions options) {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            if (containerSrc == null || containerSrc.isBlank()) {
                throw new ConfigException("containerSrc must not be null or blank");
            }
            if (hostDest == null) {
                throw new ConfigException("hostDest must not be null");
            }

            CopyOptions resolvedOptions = options == null ? CopyOptions.defaults() : options;
            NativeBindings.boxCopyOut(
                state.requireNativeHandle(),
                containerSrc,
                hostDest.toString(),
                JsonSupport.write(resolvedOptions)
            );
            return null;
        });
    }

    /** 释放原生盒子句柄，可重复调用。 */
    @Override
    public void close() {
        cleanable.clean();
    }

    private static final class BoxState implements Runnable {
        private final AtomicLong handle;

        private BoxState(long handle) {
            this.handle = new AtomicLong(handle);
        }

        private long requireNativeHandle() {
            long value = handle.get();
            if (value == 0L) {
                throw new InvalidStateException("Box handle is closed");
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
                NativeBindings.boxFree(value);
            } catch (RuntimeException ignored) {
                // Cleaner fallback must not fail on GC threads.
            }
        }
    }
}
