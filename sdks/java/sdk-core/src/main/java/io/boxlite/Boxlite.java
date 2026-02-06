package io.boxlite;

import io.boxlite.loader.NativeBindings;

/** Entry points for Java SDK runtime access. */
public final class Boxlite {
    private Boxlite() {
    }

    /** Returns the native SDK version string. */
    public static String version() {
        return NativeBindings.version();
    }

    /** Creates a new runtime handle using default options. */
    public static BoxliteRuntime newRuntime() {
        return BoxliteRuntime.create(Options.defaults());
    }

    /** Creates a new runtime handle with custom options. */
    public static BoxliteRuntime newRuntime(Options options) {
        return BoxliteRuntime.create(options);
    }

    /** Returns a handle to the process-global default runtime. */
    public static BoxliteRuntime defaultRuntime() {
        return BoxliteRuntime.defaultRuntime();
    }

    /** Initializes the process-global default runtime. Must be called before first defaultRuntime(). */
    public static void initDefaultRuntime(Options options) {
        BoxliteRuntime.initDefaultRuntime(options);
    }
}
