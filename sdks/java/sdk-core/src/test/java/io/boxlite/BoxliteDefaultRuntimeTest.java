package io.boxlite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoxliteDefaultRuntimeTest {
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
    void defaultRuntimeReturnsSingletonInstance() {
        BoxliteRuntime first = Boxlite.defaultRuntime();
        BoxliteRuntime second = Boxlite.defaultRuntime();

        assertSame(first, second, "defaultRuntime should return the same Java instance");
    }

    @Test
    void defaultRuntimeCloseIsNoOp() {
        BoxliteRuntime runtime = Boxlite.defaultRuntime();
        long handleBeforeClose = runtime.requireNativeHandle();

        runtime.close();
        long handleAfterClose = runtime.requireNativeHandle();

        assertEquals(handleBeforeClose, handleAfterClose, "default runtime handle should remain active");
        assertSame(runtime, Boxlite.defaultRuntime(), "default runtime instance should stay cached");
        assertDoesNotThrow(() -> runtime.metrics().join());
    }

    @Test
    void defaultRuntimeCanBeCalledConcurrently() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<BoxliteRuntime>> futures = new ArrayList<>();
            for (int i = 0; i < 64; i++) {
                futures.add(executor.submit(Boxlite::defaultRuntime));
            }

            BoxliteRuntime baseline = futures.get(0).get();
            for (Future<BoxliteRuntime> future : futures) {
                assertSame(baseline, future.get(), "all calls should share the same default runtime instance");
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void closeStillInvalidatesStandaloneRuntime() throws Exception {
        Path customHome = tempDir.resolve("standalone-runtime-" + UUID.randomUUID());
        Files.createDirectories(customHome);
        BoxliteRuntime runtime = Boxlite.newRuntime(Options.builder().homeDir(customHome).build());

        runtime.close();

        assertThrows(InvalidStateException.class, runtime::requireNativeHandle);
    }
}
