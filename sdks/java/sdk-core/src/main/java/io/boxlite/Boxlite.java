package io.boxlite;

import io.boxlite.loader.NativeBindings;

/** Entry points for Java SDK runtime access. */
public final class Boxlite {
    private Boxlite() {
    }

    /**
     * Returns the native SDK version string.
     *
     * @return version string
     */
    public static String version() {
        return NativeBindings.version();
    }

    /**
     * Creates a new runtime handle using default options.
     *
     * @return runtime handle
     */
    public static BoxliteRuntime newRuntime() {
        return BoxliteRuntime.create(Options.defaults());
    }

    /**
     * Creates a new runtime handle with custom options.
     *
     * @param options runtime options
     * @return runtime handle
     */
    public static BoxliteRuntime newRuntime(Options options) {
        return BoxliteRuntime.create(options);
    }

    /**
     * Returns a handle to the process-global default runtime.
     *
     * @return default runtime handle
     */
    public static BoxliteRuntime defaultRuntime() {
        return BoxliteRuntime.defaultRuntime();
    }

    /**
     * Initializes the process-global default runtime.
     *
     * <p>Call this before the first {@link #defaultRuntime()}.
     *
     * @param options default runtime options
     */
    public static void initDefaultRuntime(Options options) {
        BoxliteRuntime.initDefaultRuntime(options);
    }
}
