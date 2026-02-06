package io.boxlite;

import io.boxlite.loader.NativeBindings;

/** Entry points for minimal Java SDK functionality in Phase 0. */
public final class Boxlite {
    private Boxlite() {
    }

    /** Returns the native SDK version string. */
    public static String version() {
        return NativeBindings.version();
    }

    /** Creates a runtime handle backed by the native BoxLite runtime. */
    public static BoxliteRuntime newRuntime() {
        return BoxliteRuntime.create();
    }
}
