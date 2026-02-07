package io.boxlite.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.boxlite.BoxliteException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    void stagesOverrideLibraryIntoTempDir() throws Exception {
        Path overrideLibrary = Files.createTempFile("boxlite-java-native", ".so");
        Files.writeString(overrideLibrary, "fake-native", StandardCharsets.UTF_8);
        Path tempDir = Files.createTempDirectory("boxlite-java-loader-test");

        Path staged = NativeLoader.stageNativeLibrary(
            tempDir,
            "linux-x86_64",
            overrideLibrary.toString()
        );

        assertEquals(tempDir.resolve(overrideLibrary.getFileName()), staged);
        assertEquals("fake-native", Files.readString(staged, StandardCharsets.UTF_8));
    }

    @Test
    void rejectsMissingOverrideLibrary() throws Exception {
        Path tempDir = Files.createTempDirectory("boxlite-java-loader-test");
        Path missing = tempDir.resolve("missing-native-lib.so");

        BoxliteException error = assertThrows(
            BoxliteException.class,
            () -> NativeLoader.stageNativeLibrary(tempDir, "linux-x86_64", missing.toString())
        );

        assertTrue(error.getMessage().contains("does not exist"), "Expected a missing path message");
    }
}
