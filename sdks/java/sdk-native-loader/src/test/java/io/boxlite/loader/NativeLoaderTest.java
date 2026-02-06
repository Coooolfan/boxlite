package io.boxlite.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.boxlite.BoxliteException;
import org.junit.jupiter.api.Test;

class NativeLoaderTest {
    @Test
    void mapsMacArm64() {
        assertEquals("darwin-aarch64", NativeLoader.toPlatformId("Mac OS X", "arm64"));
    }

    @Test
    void mapsLinuxAmd64() {
        assertEquals("linux-x86_64", NativeLoader.toPlatformId("Linux", "amd64"));
    }

    @Test
    void rejectsUnsupportedOperatingSystem() {
        assertThrows(BoxliteException.class, () -> NativeLoader.toPlatformId("Windows 11", "amd64"));
    }

    @Test
    void rejectsUnsupportedArchitecture() {
        assertThrows(BoxliteException.class, () -> NativeLoader.toPlatformId("Linux", "ppc64"));
    }
}
