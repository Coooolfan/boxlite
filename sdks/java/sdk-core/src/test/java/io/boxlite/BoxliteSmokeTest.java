package io.boxlite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BoxliteSmokeTest {
    private static final int LOG_TAIL_LINES = 120;
    private static final Path SHARED_RUNTIME_HOME = Path.of(
        System.getProperty("user.home"),
        ".boxlite"
    );

    @TempDir
    Path tempDir;

    @BeforeAll
    static void verifyVmPreflight() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "");
        String normalizedArch = osArch.toLowerCase(Locale.ROOT);

        if (!osName.contains("mac")) {
            return;
        }

        if (!"aarch64".equals(normalizedArch) && !"arm64".equals(normalizedArch)) {
            fail(
                "Java VM smoke tests require macOS arm64. " +
                "Detected os.arch=" + osArch
            );
        }

        String hvSupport = runCommand("sysctl", "-n", "kern.hv_support").trim();
        if (!"1".equals(hvSupport)) {
            fail(
                "Hypervisor.framework is unavailable (kern.hv_support=" + hvSupport + "). " +
                "Enable virtualization support before running tests."
            );
        }
    }

    @Test
    void versionIsAvailable() {
        String version = Boxlite.version();
        assertFalse(version.isBlank(), "Version should not be blank");
    }

    @Test
    void runtimeCanCreateGetListAndRemoveBoxes() {
        try (TestRuntime fixture = newRuntimeForTest("case-create-get-remove")) {
            BoxliteRuntime runtime = fixture.runtime();
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
        try (TestRuntime fixture = newRuntimeForTest("case-get-or-create")) {
            BoxliteRuntime runtime = fixture.runtime();
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
    void boxCanStartStopAndBeReattached() throws Exception {
        runVmTest("case-start-stop", runtime -> {
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
        });
    }

    @Test
    void copyInOutRoundTripWorks() throws Exception {
        runVmTest("case-copy", runtime -> {
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
        });
    }

    @Test
    void execSupportsEnvWorkingDirAndWait() throws Exception {
        runVmTest("case-exec-env", runtime -> {
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
        });
    }

    @Test
    void execStdinRoundTripWorks() throws Exception {
        runVmTest("case-exec-stdin", runtime -> {
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
        });
    }

    @Test
    void execCanBeKilled() throws Exception {
        runVmTest("case-exec-kill", runtime -> {
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
        });
    }

    @Test
    void execWaitAndKillCanRunConcurrently() throws Exception {
        runVmTest("case-exec-wait-kill-concurrent", runtime -> {
            BoxOptions options = BoxOptions.builder()
                .autoRemove(false)
                .build();
            BoxHandle box = runtime.create(
                options,
                "java-phase2-exec-kill-concurrent-" + UUID.randomUUID()
            ).join();
            ExecutionHandle exec = box.exec(
                ExecCommand.builder("sh")
                    .addArg("-lc")
                    .addArg("sleep 30")
                    .build()
            ).join();

            CompletableFuture<ExecResult> waitFuture = exec.waitFor();
            Thread.sleep(250);
            exec.kill().join();

            ExecResult result = waitFuture.orTimeout(10, TimeUnit.SECONDS).join();
            assertFalse(result.success(), "concurrently killed command should not succeed");

            exec.close();
            runtime.remove(box.id(), true).join();
            box.close();
        });
    }

    @Test
    void runtimeRemoveMissingBoxThrowsNotFound() {
        try (TestRuntime fixture = newRuntimeForTest("case-remove-missing")) {
            BoxliteRuntime runtime = fixture.runtime();
            RuntimeException cause = joinFailure(
                runtime.remove("missing-" + UUID.randomUUID(), false)
            );
            assertInstanceOf(NotFoundException.class, cause);
        }
    }

    @Test
    void closedHandleOperationsThrowInvalidState() {
        try (TestRuntime fixture = newRuntimeForTest("case-handle-close")) {
            BoxliteRuntime runtime = fixture.runtime();
            BoxHandle box = runtime.create(BoxOptions.defaults(), "java-phase1-close-" + UUID.randomUUID()).join();
            box.close();

            RuntimeException cause = joinFailure(box.start());
            assertInstanceOf(InvalidStateException.class, cause);
        }
    }

    private void runVmTest(String suffix, VmScenario scenario) throws Exception {
        try (TestRuntime fixture = newRuntimeForTest(suffix)) {
            try {
                scenario.run(fixture.runtime());
            } catch (Exception | AssertionError failure) {
                dumpVmDiagnostics(suffix, fixture.homeDir(), failure);
                throw failure;
            }
        }
    }

    private TestRuntime newRuntimeForTest(String suffix) {
        try {
            Files.createDirectories(SHARED_RUNTIME_HOME);
        } catch (IOException e) {
            throw new RuntimeException(
                "Failed to prepare shared runtime home " + SHARED_RUNTIME_HOME,
                e
            );
        }

        Options options = Options.builder()
            .homeDir(SHARED_RUNTIME_HOME)
            .build();
        return new TestRuntime(Boxlite.newRuntime(options), SHARED_RUNTIME_HOME);
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

    private static void dumpVmDiagnostics(String caseName, Path runtimeHome, Throwable failure) {
        System.err.println();
        System.err.println("=== BoxLite Java VM Diagnostics ===");
        System.err.println("Case: " + caseName);
        System.err.println("Runtime home: " + runtimeHome);
        System.err.println(
            "Failure: " + failure.getClass().getName() + ": " + failure.getMessage()
        );

        Path logsDir = runtimeHome.resolve("logs");
        if (!Files.isDirectory(logsDir)) {
            System.err.println("Logs directory not found: " + logsDir);
            System.err.println("===================================");
            return;
        }

        printLatestLogTail(logsDir, "boxlite.log");
        printLatestLogTail(logsDir, "boxlite-shim.log");
        System.err.println("===================================");
    }

    private static void printLatestLogTail(Path logsDir, String prefix) {
        Optional<Path> latest = findLatestLogFile(logsDir, prefix);
        if (latest.isEmpty()) {
            System.err.println("No log files found for prefix: " + prefix);
            return;
        }

        Path logFile = latest.get();
        System.err.println("-- tail -n " + LOG_TAIL_LINES + " " + logFile + " --");
        try {
            List<String> lines = Files.readAllLines(logFile, StandardCharsets.UTF_8);
            int from = Math.max(0, lines.size() - LOG_TAIL_LINES);
            for (int i = from; i < lines.size(); i++) {
                System.err.println(lines.get(i));
            }
        } catch (IOException e) {
            System.err.println("Failed to read log file " + logFile + ": " + e.getMessage());
        }
    }

    private static Optional<Path> findLatestLogFile(Path logsDir, String prefix) {
        try (Stream<Path> stream = Files.list(logsDir)) {
            return stream
                .filter(Files::isRegularFile)
                .filter(path -> path.getFileName().toString().startsWith(prefix))
                .max(Comparator.comparingLong(BoxliteSmokeTest::lastModifiedMillis));
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private static long lastModifiedMillis(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return Long.MIN_VALUE;
        }
    }

    private static String runCommand(String... command) {
        Process process;
        try {
            process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
        } catch (IOException e) {
            fail("Failed to run preflight command: " + String.join(" ", command), e);
            return "";
        }

        String output;
        try {
            output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            fail("Failed to read preflight command output: " + String.join(" ", command), e);
            return "";
        }

        int exitCode;
        try {
            exitCode = process.waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Interrupted while waiting for preflight command", e);
            return "";
        }

        if (exitCode != 0) {
            fail(
                "Preflight command failed (" + exitCode + "): " +
                String.join(" ", command) + "\n" + output
            );
        }
        return output;
    }

    @FunctionalInterface
    private interface VmScenario {
        void run(BoxliteRuntime runtime) throws Exception;
    }

    private static final class TestRuntime implements AutoCloseable {
        private final BoxliteRuntime runtime;
        private final Path homeDir;

        private TestRuntime(BoxliteRuntime runtime, Path homeDir) {
            this.runtime = runtime;
            this.homeDir = homeDir;
        }

        private BoxliteRuntime runtime() {
            return runtime;
        }

        private Path homeDir() {
            return homeDir;
        }

        @Override
        public void close() {
            runtime.close();
        }
    }
}
