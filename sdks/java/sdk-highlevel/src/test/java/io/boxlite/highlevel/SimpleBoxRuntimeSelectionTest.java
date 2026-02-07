package io.boxlite.highlevel;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

import io.boxlite.Boxlite;
import io.boxlite.BoxliteException;
import io.boxlite.BoxliteRuntime;
import io.boxlite.Options;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimpleBoxRuntimeSelectionTest {
    @TempDir
    static Path tempDir;

    @BeforeAll
    static void initDefaultRuntimeWithIsolatedHome() throws Exception {
        Path homeDir = tempDir.resolve("default-runtime-home");
        Files.createDirectories(homeDir);
        try {
            Boxlite.initDefaultRuntime(Options.builder().homeDir(homeDir).build());
        } catch (BoxliteException ex) {
            String message = ex.getMessage();
            if (message == null || !message.toLowerCase(Locale.ROOT).contains("already initialized")) {
                throw ex;
            }
        }
    }

    @Test
    void usesProcessDefaultRuntimeWhenRuntimeOptionIsMissing() throws Exception {
        BoxliteRuntime defaultRuntime = Boxlite.defaultRuntime();

        try (SimpleBox box = new SimpleBox(SimpleBoxOptions.builder().build())) {
            Field runtimeField = SimpleBox.class.getDeclaredField("runtime");
            runtimeField.setAccessible(true);
            BoxliteRuntime runtime = (BoxliteRuntime) runtimeField.get(box);

            Field ownsRuntimeField = SimpleBox.class.getDeclaredField("ownsRuntime");
            ownsRuntimeField.setAccessible(true);
            boolean ownsRuntime = ownsRuntimeField.getBoolean(box);

            assertSame(defaultRuntime, runtime, "SimpleBox should reuse process default runtime");
            assertFalse(ownsRuntime, "SimpleBox should not own the process default runtime");
        }
    }
}
