package io.boxlite;

import io.boxlite.loader.NativeBindings;
import java.lang.ref.Cleaner;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Handle to a command execution inside a BoxLite box. */
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

    public String id() {
        return id;
    }

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

    public CompletableFuture<Void> stdinClose() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            NativeBindings.executionStdinClose(state.requireNativeHandle());
            return null;
        });
    }

    public CompletableFuture<Optional<String>> stdoutNextLine() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            String line = NativeBindings.executionStdoutNextLine(state.requireNativeHandle());
            return Optional.ofNullable(line);
        });
    }

    public CompletableFuture<Optional<String>> stderrNextLine() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            String line = NativeBindings.executionStderrNextLine(state.requireNativeHandle());
            return Optional.ofNullable(line);
        });
    }

    public CompletableFuture<ExecResult> waitFor() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            String json = NativeBindings.executionWait(state.requireNativeHandle());
            return JsonSupport.read(json, ExecResult.class);
        });
    }

    public CompletableFuture<Void> kill() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            NativeBindings.executionKill(state.requireNativeHandle());
            return null;
        });
    }

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
