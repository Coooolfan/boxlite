package io.boxlite;

import io.boxlite.loader.NativeBindings;
import java.lang.ref.Cleaner;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** 盒子内命令执行句柄。 */
public final class ExecutionHandle implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();

    private final BoxliteRuntime runtime;
    private final ExecutionState state;
    private final String id;
    private final Cleaner.Cleanable cleanable;

    ExecutionHandle(BoxliteRuntime runtime, long nativeHandle) {
        this.runtime = runtime;
        this.state = new ExecutionState(nativeHandle);
        this.id = NativeBindings.executionId(nativeHandle);
        this.cleanable = CLEANER.register(this, state);
    }

    /**
     * 返回执行 ID。
     *
     * @return 执行 ID。
     */
    public String id() {
        return id;
    }

    /**
     * 向进程标准输入写入字节。
     *
     * @param data 待写入字节。
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> stdinWrite(byte[] data) {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            if (data == null) {
                throw new ConfigException("data must not be null");
            }
            NativeBindings.executionStdinWrite(state.requireNativeHandle(), data);
            return null;
        });
    }

    /**
     * 关闭进程标准输入。
     *
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> stdinClose() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            NativeBindings.executionStdinClose(state.requireNativeHandle());
            return null;
        });
    }

    /**
     * 读取标准输出的下一行。
     *
     * @return 异步返回下一行；流结束时为空。
     */
    public CompletableFuture<Optional<String>> stdoutNextLine() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            String line = NativeBindings.executionStdoutNextLine(state.requireNativeHandle());
            return Optional.ofNullable(line);
        });
    }

    /**
     * 读取标准错误的下一行。
     *
     * @return 异步返回下一行；流结束时为空。
     */
    public CompletableFuture<Optional<String>> stderrNextLine() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            String line = NativeBindings.executionStderrNextLine(state.requireNativeHandle());
            return Optional.ofNullable(line);
        });
    }

    /**
     * 等待进程结束。
     *
     * @return 异步返回执行结果。
     */
    public CompletableFuture<ExecResult> waitFor() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            String json = NativeBindings.executionWait(state.requireNativeHandle());
            return JsonSupport.read(json, ExecResult.class);
        });
    }

    /**
     * 终止正在运行的进程。
     *
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> kill() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            NativeBindings.executionKill(state.requireNativeHandle());
            return null;
        });
    }

    /**
     * 在 TTY 模式下调整终端大小。
     *
     * @param rows 终端行数，必须 {@code > 0}。
     * @param cols 终端列数，必须 {@code > 0}。
     * @return 异步完成信号。
     */
    public CompletableFuture<Void> resizeTty(int rows, int cols) {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            if (rows <= 0 || cols <= 0) {
                throw new ConfigException("rows and cols must both be > 0");
            }
            NativeBindings.executionResizeTty(state.requireNativeHandle(), rows, cols);
            return null;
        });
    }

    /** 释放原生执行句柄，可重复调用。 */
    @Override
    public void close() {
        cleanable.clean();
    }

    private static final class ExecutionState implements Runnable {
        private final AtomicLong handle;

        private ExecutionState(long handle) {
            this.handle = new AtomicLong(handle);
        }

        private long requireNativeHandle() {
            long value = handle.get();
            if (value == 0L) {
                throw new InvalidStateException("Execution handle is closed");
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
                NativeBindings.executionFree(value);
            } catch (RuntimeException ignored) {
                // Cleaner fallback must not fail on GC threads.
            }
        }
    }
}
