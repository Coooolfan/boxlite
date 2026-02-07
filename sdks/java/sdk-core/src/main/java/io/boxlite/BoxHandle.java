package io.boxlite;

import io.boxlite.loader.NativeBindings;
import java.lang.ref.Cleaner;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;

/** Handle to a native BoxLite box instance. */
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

    public String id() {
        return id;
    }

    public Optional<String> name() {
        return name;
    }

    public BoxInfo info() {
        runtime.requireNativeHandle();
        String json = NativeBindings.boxInfo(state.requireNativeHandle());
        return JsonSupport.read(json, BoxInfo.class);
    }

    public CompletableFuture<Void> start() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            NativeBindings.boxStart(state.requireNativeHandle());
            return null;
        });
    }

    public CompletableFuture<Void> stop() {
        return runtime.async(() -> {
            runtime.requireNativeHandle();
            NativeBindings.boxStop(state.requireNativeHandle());
            return null;
        });
    }

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
