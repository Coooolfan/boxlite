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

/** Runtime handle for creating and managing BoxLite boxes. */
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
     * Creates an independent runtime with default options.
     *
     * @return a new runtime handle
     */
    public static BoxliteRuntime create() {
        return create(Options.defaults());
    }

    /**
     * Creates an independent runtime with custom options.
     *
     * @param options runtime configuration; {@code null} means {@link Options#defaults()}
     * @return a new runtime handle
     */
    public static BoxliteRuntime create(Options options) {
        Options resolvedOptions = options == null ? Options.defaults() : options;
        long handle = NativeBindings.runtimeNew(JsonSupport.write(toRuntimePayload(resolvedOptions)));
        return fromNativeHandle(handle, "runtimeNew");
    }

    /**
     * Returns a handle to the process-global default runtime.
     *
     * @return default runtime handle
     */
    public static BoxliteRuntime defaultRuntime() {
        long handle = NativeBindings.runtimeDefault();
        return fromNativeHandle(handle, "runtimeDefault");
    }

    /**
     * Initializes the process-global default runtime.
     *
     * <p>Call this before the first {@link #defaultRuntime()} to override defaults.
     *
     * @param options runtime configuration; {@code null} means {@link Options#defaults()}
     */
    public static void initDefaultRuntime(Options options) {
        Options resolvedOptions = options == null ? Options.defaults() : options;
        NativeBindings.runtimeInitDefault(JsonSupport.write(toRuntimePayload(resolvedOptions)));
    }

    /**
     * Creates a box.
     *
     * @param options box options; {@code null} means {@link BoxOptions#defaults()}
     * @param name optional box name; may be {@code null}
     * @return async handle for the created box
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
     * Creates a box without an explicit name.
     *
     * @param options box options; {@code null} means {@link BoxOptions#defaults()}
     * @return async handle for the created box
     */
    public CompletableFuture<BoxHandle> create(BoxOptions options) {
        return create(options, null);
    }

    /**
     * Looks up a box by name and creates one when absent.
     *
     * @param options box options used when creation is needed
     * @param name box name used as lookup key; may be {@code null}
     * @return async result containing box handle and created flag
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
     * Creates or gets an unnamed box.
     *
     * @param options box options used when creation is needed
     * @return async result containing box handle and created flag
     */
    public CompletableFuture<GetOrCreateResult> getOrCreate(BoxOptions options) {
        return getOrCreate(options, null);
    }

    /**
     * Gets a box handle by id or name.
     *
     * @param idOrName box id or box name
     * @return async optional handle; empty when not found
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
     * Gets box metadata by id or name.
     *
     * @param idOrName box id or box name
     * @return async optional metadata; empty when not found
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
     * Lists metadata for all known boxes.
     *
     * @return async list of boxes
     */
    public CompletableFuture<List<BoxInfo>> listInfo() {
        return async(() -> {
            String json = NativeBindings.runtimeListInfo(requireNativeHandle());
            return JsonSupport.readList(json, BoxInfo.class);
        });
    }

    /**
     * Removes a box by id or name.
     *
     * @param idOrName box id or box name
     * @param force force removal for running box
     * @return async completion
     */
    public CompletableFuture<Void> remove(String idOrName, boolean force) {
        requireIdOrName(idOrName);
        return async(() -> {
            NativeBindings.runtimeRemove(requireNativeHandle(), idOrName, force);
            return null;
        });
    }

    /**
     * Removes a box by id or name without forcing.
     *
     * @param idOrName box id or box name
     * @return async completion
     */
    public CompletableFuture<Void> remove(String idOrName) {
        return remove(idOrName, false);
    }

    /**
     * Reads runtime metrics.
     *
     * @return async metrics snapshot
     */
    public CompletableFuture<RuntimeMetrics> metrics() {
        return async(() -> {
            String json = NativeBindings.runtimeMetrics(requireNativeHandle());
            return JsonSupport.read(json, RuntimeMetrics.class);
        });
    }

    /**
     * Shuts down the runtime.
     *
     * @param timeoutSeconds optional graceful timeout in seconds; {@code null} uses default
     * @return async completion
     */
    public CompletableFuture<Void> shutdown(Integer timeoutSeconds) {
        return async(() -> {
            NativeBindings.runtimeShutdown(requireNativeHandle(), timeoutSeconds);
            return null;
        });
    }

    /** Releases the native runtime handle. Safe to call multiple times. */
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
