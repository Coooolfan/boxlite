package io.boxlite.loader;

import io.boxlite.BoxliteException;

/** JNI bridge for Phase 0 native calls. */
public final class NativeBindings {
    private static final int EXPECTED_ABI_VERSION = 1;

    static {
        NativeLoader.load();
        int actual = nativeAbiVersion();
        if (actual != EXPECTED_ABI_VERSION) {
            throw new BoxliteException(
                "Native ABI version mismatch: expected " + EXPECTED_ABI_VERSION + ", got " + actual
            );
        }
    }

    private NativeBindings() {
    }

    public static String version() {
        return nativeVersion();
    }

    public static long runtimeNew() {
        return nativeRuntimeNew();
    }

    public static void runtimeFree(long handle) {
        nativeRuntimeFree(handle);
    }

    public static int abiVersion() {
        return nativeAbiVersion();
    }

    private static native String nativeVersion();

    private static native long nativeRuntimeNew();

    private static native void nativeRuntimeFree(long handle);

    private static native int nativeAbiVersion();
}
