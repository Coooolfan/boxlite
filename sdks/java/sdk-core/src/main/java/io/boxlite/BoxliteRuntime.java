package io.boxlite;

import io.boxlite.loader.NativeBindings;
import java.lang.ref.Cleaner;
import java.util.concurrent.atomic.AtomicLong;

/** Minimal runtime handle wrapper for Phase 0. */
public final class BoxliteRuntime implements AutoCloseable {
    private static final Cleaner CLEANER = Cleaner.create();

    private final RuntimeState state;
    private final Cleaner.Cleanable cleanable;

    private BoxliteRuntime(long nativeHandle) {
        this.state = new RuntimeState(nativeHandle);
        this.cleanable = CLEANER.register(this, state);
    }

    public static BoxliteRuntime create() {
        long handle = NativeBindings.runtimeNew();
        if (handle == 0L) {
            throw new BoxliteException("Native runtime returned invalid handle 0");
        }
        return new BoxliteRuntime(handle);
    }

    @Override
    public void close() {
        cleanable.clean();
    }

    private static final class RuntimeState implements Runnable {
        private final AtomicLong handle;

        private RuntimeState(long handle) {
            this.handle = new AtomicLong(handle);
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
                // Cleaner fallback should never crash application threads.
            }
        }
    }
}
