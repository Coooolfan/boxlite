package io.boxlite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoxliteSmokeTest {
    @TempDir
    Path tempDir;

    @Test
    void versionIsAvailable() {
        String version = Boxlite.version();
        assertFalse(version.isBlank(), "Version should not be blank");
    }

    @Test
    void runtimeCanCreateGetListAndRemoveBoxes() {
        try (BoxliteRuntime runtime = newRuntimeForTest("case-create-get-remove")) {
            String name = "java-phase1-" + UUID.randomUUID();

            BoxHandle created = runtime.create(BoxOptions.defaults(), name).join();
            assertNotNull(created.id());
            assertEquals(Optional.of(name), created.name());

            Optional<BoxHandle> byId = runtime.get(created.id()).join();
            assertTrue(byId.isPresent(), "Expected box to be available by id");

            Optional<BoxInfo> info = runtime.getInfo(name).join();
            assertTrue(info.isPresent(), "Expected box info to be available by name");
            assertEquals(name, info.get().name());

            assertTrue(
                runtime.listInfo().join().stream().anyMatch(box -> box.id().equals(created.id())),
                "Expected listInfo() to include created box"
            );

            runtime.remove(created.id(), false).join();
            assertTrue(runtime.get(created.id()).join().isEmpty(), "Box should be removed");

            created.close();
            byId.ifPresent(BoxHandle::close);
        }
    }

    @Test
    void runtimeSupportsGetOrCreateMetricsAndShutdown() {
        try (BoxliteRuntime runtime = newRuntimeForTest("case-get-or-create")) {
            String name = "java-phase1-goc-" + UUID.randomUUID();

            GetOrCreateResult first = runtime.getOrCreate(BoxOptions.defaults(), name).join();
            GetOrCreateResult second = runtime.getOrCreate(BoxOptions.defaults(), name).join();

            assertTrue(first.created(), "First getOrCreate call should create the box");
            assertFalse(second.created(), "Second getOrCreate call should return existing box");
            assertEquals(first.box().id(), second.box().id(), "Expected same box handle");

            RuntimeMetrics metrics = runtime.metrics().join();
            assertNotNull(metrics, "Runtime metrics should be available");

            first.box().close();
            second.box().close();
            assertDoesNotThrow(() -> runtime.shutdown(1).join());
        }
    }

    @Test
    void boxCanStartStopAndBeReattached() {
        requireVmIntegration();
        try (BoxliteRuntime runtime = newRuntimeForTest("case-start-stop")) {
            BoxOptions options = BoxOptions.builder()
                .autoRemove(false)
                .build();
            BoxHandle box = runtime.create(options, "java-phase1-start-stop-" + UUID.randomUUID()).join();

            box.start().join();
            BoxInfo runningInfo = box.info();
            assertTrue(runningInfo.state().running(), "Box should be running after start()");

            box.stop().join();
            Optional<BoxHandle> reattached = runtime.get(box.id()).join();
            assertTrue(reattached.isPresent(), "Stopped box with autoRemove=false should be reattachable");

            reattached.get().close();
            runtime.remove(box.id(), true).join();
            box.close();
        }
    }

    @Test
    void copyInOutRoundTripWorks() throws Exception {
        requireVmIntegration();
        try (BoxliteRuntime runtime = newRuntimeForTest("case-copy")) {
            BoxOptions options = BoxOptions.builder()
                .autoRemove(false)
                .build();
            BoxHandle box = runtime.create(options, "java-phase1-copy-" + UUID.randomUUID()).join();

            Path hostSrc = tempDir.resolve("copy-src.txt");
            String payload = "phase1-copy-" + UUID.randomUUID();
            Files.writeString(hostSrc, payload);

            String guestPath = "/root/java-copy-roundtrip/copy-src.txt";
            box.copyIn(hostSrc, guestPath, CopyOptions.defaults()).join();

            Path hostOutDir = tempDir.resolve("copy-out");
            Files.createDirectories(hostOutDir);
            box.copyOut(guestPath, hostOutDir, CopyOptions.defaults()).join();

            Path copiedFile;
            try (var walk = Files.walk(hostOutDir)) {
                copiedFile = walk
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equals("copy-src.txt"))
                    .findFirst()
                    .orElseThrow();
            }
            assertEquals(payload, Files.readString(copiedFile));

            runtime.remove(box.id(), true).join();
            box.close();
        }
    }

    @Test
    void execSupportsEnvWorkingDirAndWait() {
        requireVmIntegration();
        try (BoxliteRuntime runtime = newRuntimeForTest("case-exec-env")) {
            BoxOptions options = BoxOptions.builder()
                .autoRemove(false)
                .build();
            BoxHandle box = runtime.create(options, "java-phase2-exec-env-" + UUID.randomUUID()).join();
            ExecutionHandle exec = box.exec(
                ExecCommand.builder("sh")
                    .addArg("-lc")
                    .addArg("printf '%s|%s\\n' \"$BOXLITE_JAVA_PHASE2\" \"$PWD\"")
                    .putEnv("BOXLITE_JAVA_PHASE2", "ok")
                    .workingDir("/")
                    .build()
            ).join();

            String line = exec.stdoutNextLine().join().orElseThrow();
            ExecResult result = exec.waitFor().join();

            assertEquals("ok|/", line.strip());
            assertTrue(result.success(), "execution should succeed");

            exec.close();
            runtime.remove(box.id(), true).join();
            box.close();
        }
    }

    @Test
    void execStdinRoundTripWorks() {
        requireVmIntegration();
        try (BoxliteRuntime runtime = newRuntimeForTest("case-exec-stdin")) {
            BoxOptions options = BoxOptions.builder()
                .autoRemove(false)
                .build();
            BoxHandle box = runtime.create(options, "java-phase2-exec-stdin-" + UUID.randomUUID()).join();
            ExecutionHandle exec = box.exec(ExecCommand.builder("cat").build()).join();

            exec.stdinWrite("hello-stdin\n".getBytes(StandardCharsets.UTF_8)).join();
            exec.stdinClose().join();
            String line = exec.stdoutNextLine().join().orElseThrow();
            ExecResult result = exec.waitFor().join();

            assertEquals("hello-stdin", line.strip());
            assertTrue(result.success(), "cat should exit successfully");

            exec.close();
            runtime.remove(box.id(), true).join();
            box.close();
        }
    }

    @Test
    void execCanBeKilled() {
        requireVmIntegration();
        try (BoxliteRuntime runtime = newRuntimeForTest("case-exec-kill")) {
            BoxOptions options = BoxOptions.builder()
                .autoRemove(false)
                .build();
            BoxHandle box = runtime.create(options, "java-phase2-exec-kill-" + UUID.randomUUID()).join();
            ExecutionHandle exec = box.exec(
                ExecCommand.builder("sh")
                    .addArg("-lc")
                    .addArg("sleep 30")
                    .build()
            ).join();

            exec.kill().join();
            ExecResult result = exec.waitFor().join();
            assertFalse(result.success(), "killed command should not succeed");

            exec.close();
            runtime.remove(box.id(), true).join();
            box.close();
        }
    }

    @Test
    void runtimeRemoveMissingBoxThrowsNotFound() {
        try (BoxliteRuntime runtime = newRuntimeForTest("case-remove-missing")) {
            RuntimeException cause = joinFailure(
                runtime.remove("missing-" + UUID.randomUUID(), false)
            );
            assertInstanceOf(NotFoundException.class, cause);
        }
    }

    @Test
    void closedHandleOperationsThrowInvalidState() {
        try (BoxliteRuntime runtime = newRuntimeForTest("case-handle-close")) {
            BoxHandle box = runtime.create(BoxOptions.defaults(), "java-phase1-close-" + UUID.randomUUID()).join();
            box.close();

            RuntimeException cause = joinFailure(box.start());
            assertInstanceOf(InvalidStateException.class, cause);
        }
    }

    private BoxliteRuntime newRuntimeForTest(String suffix) {
        Path runtimeHome;
        try {
            String token = UUID.randomUUID().toString().substring(0, 8);
            runtimeHome = Path.of("/tmp", "blj-" + token);
            Files.createDirectories(runtimeHome);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create temporary runtime home", e);
        }

        Options options = Options.builder()
            .homeDir(runtimeHome)
            .build();
        return Boxlite.newRuntime(options);
    }

    private static RuntimeException joinFailure(java.util.concurrent.CompletableFuture<?> future) {
        try {
            future.join();
            fail("Expected future to fail");
            return new RuntimeException("unreachable");
        } catch (CompletionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                return runtimeException;
            }
            throw new RuntimeException("Unexpected checked exception", cause);
        }
    }

    private static void requireVmIntegration() {
        boolean enabledByEnv = "1".equals(System.getenv("BOXLITE_JAVA_RUN_VM_TESTS"));
        boolean enabledByProperty = Boolean.getBoolean("boxlite.java.runVmTests");
        Assumptions.assumeTrue(
            enabledByEnv || enabledByProperty,
            "VM integration tests are disabled. Set BOXLITE_JAVA_RUN_VM_TESTS=1 to enable."
        );
    }
}
