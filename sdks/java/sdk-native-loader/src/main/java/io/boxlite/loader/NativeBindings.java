package io.boxlite.loader;

import io.boxlite.BoxliteException;

/** JNI bridge for runtime and box lifecycle operations. */
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

    public static long runtimeNew(String optionsJson) {
        return nativeRuntimeNew(optionsJson);
    }

    public static long runtimeDefault() {
        return nativeRuntimeDefault();
    }

    public static void runtimeInitDefault(String optionsJson) {
        nativeRuntimeInitDefault(optionsJson);
    }

    public static void runtimeFree(long runtimeHandle) {
        nativeRuntimeFree(runtimeHandle);
    }

    public static long runtimeCreate(long runtimeHandle, String boxOptionsJson, String name) {
        return nativeRuntimeCreate(runtimeHandle, boxOptionsJson, name);
    }

    public static long[] runtimeGetOrCreate(long runtimeHandle, String boxOptionsJson, String name) {
        return nativeRuntimeGetOrCreate(runtimeHandle, boxOptionsJson, name);
    }

    public static long runtimeGet(long runtimeHandle, String idOrName) {
        return nativeRuntimeGet(runtimeHandle, idOrName);
    }

    public static String runtimeGetInfo(long runtimeHandle, String idOrName) {
        return nativeRuntimeGetInfo(runtimeHandle, idOrName);
    }

    public static String runtimeListInfo(long runtimeHandle) {
        return nativeRuntimeListInfo(runtimeHandle);
    }

    public static void runtimeRemove(long runtimeHandle, String idOrName, boolean force) {
        nativeRuntimeRemove(runtimeHandle, idOrName, force);
    }

    public static String runtimeMetrics(long runtimeHandle) {
        return nativeRuntimeMetrics(runtimeHandle);
    }

    public static void runtimeShutdown(long runtimeHandle, Integer timeoutSeconds) {
        nativeRuntimeShutdown(runtimeHandle, timeoutSeconds);
    }

    public static void boxFree(long boxHandle) {
        nativeBoxFree(boxHandle);
    }

    public static String boxId(long boxHandle) {
        return nativeBoxId(boxHandle);
    }

    public static String boxName(long boxHandle) {
        return nativeBoxName(boxHandle);
    }

    public static String boxInfo(long boxHandle) {
        return nativeBoxInfo(boxHandle);
    }

    public static void boxStart(long boxHandle) {
        nativeBoxStart(boxHandle);
    }

    public static void boxStop(long boxHandle) {
        nativeBoxStop(boxHandle);
    }

    public static void boxCopyIn(
        long boxHandle,
        String hostPath,
        String containerDest,
        String copyOptionsJson
    ) {
        nativeBoxCopyIn(boxHandle, hostPath, containerDest, copyOptionsJson);
    }

    public static void boxCopyOut(
        long boxHandle,
        String containerSrc,
        String hostDest,
        String copyOptionsJson
    ) {
        nativeBoxCopyOut(boxHandle, containerSrc, hostDest, copyOptionsJson);
    }

    public static int abiVersion() {
        return nativeAbiVersion();
    }

    private static native String nativeVersion();

    private static native long nativeRuntimeNew(String optionsJson);

    private static native long nativeRuntimeDefault();

    private static native void nativeRuntimeInitDefault(String optionsJson);

    private static native void nativeRuntimeFree(long runtimeHandle);

    private static native long nativeRuntimeCreate(long runtimeHandle, String boxOptionsJson, String name);

    private static native long[] nativeRuntimeGetOrCreate(long runtimeHandle, String boxOptionsJson, String name);

    private static native long nativeRuntimeGet(long runtimeHandle, String idOrName);

    private static native String nativeRuntimeGetInfo(long runtimeHandle, String idOrName);

    private static native String nativeRuntimeListInfo(long runtimeHandle);

    private static native void nativeRuntimeRemove(long runtimeHandle, String idOrName, boolean force);

    private static native String nativeRuntimeMetrics(long runtimeHandle);

    private static native void nativeRuntimeShutdown(long runtimeHandle, Integer timeoutSeconds);

    private static native void nativeBoxFree(long boxHandle);

    private static native String nativeBoxId(long boxHandle);

    private static native String nativeBoxName(long boxHandle);

    private static native String nativeBoxInfo(long boxHandle);

    private static native void nativeBoxStart(long boxHandle);

    private static native void nativeBoxStop(long boxHandle);

    private static native void nativeBoxCopyIn(
        long boxHandle,
        String hostPath,
        String containerDest,
        String copyOptionsJson
    );

    private static native void nativeBoxCopyOut(
        long boxHandle,
        String containerSrc,
        String hostDest,
        String copyOptionsJson
    );

    private static native int nativeAbiVersion();
}
