package io.boxlite.highlevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.boxlite.BoxOptions;
import io.boxlite.BoxliteRuntime;
import io.boxlite.ConfigException;
import io.boxlite.Options;
import io.boxlite.RuntimeMetrics;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CodeBoxVmTest {
    @BeforeAll
    static void verifyVmPreflight() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "");
        String normalizedArch = osArch.toLowerCase(Locale.ROOT);

        if (!osName.contains("mac")) {
            return;
        }

        if (!"aarch64".equals(normalizedArch) && !"arm64".equals(normalizedArch)) {
            fail("CodeBox VM tests require macOS arm64. Detected os.arch=" + osArch);
        }

        String hvSupport = runCommand("sysctl", "-n", "kern.hv_support").trim();
        if (!"1".equals(hvSupport)) {
            fail("Hypervisor.framework is unavailable (kern.hv_support=" + hvSupport + ")");
        }
    }

    @Test
    void codeBoxRunExecutesPythonCode() {
        try (CodeBox box = new CodeBox().start()) {
            assertThrows(ConfigException.class, () -> box.run(" "));
            assertThrows(ConfigException.class, () -> box.installPackage(""));
            assertThrows(ConfigException.class, () -> box.installPackages("requests", null));

            ExecOutput output = box.run(
                "import os; print('ok|' + os.environ.get('BOXLITE_CODEBOX', 'missing'))",
                Map.of("BOXLITE_CODEBOX", "env")
            );

            assertTrue(output.success(), "expected python execution success");
            assertEquals("ok|env", output.stdout().strip());
        }
    }

    @Test
    void codeBoxRunReportsNonZeroExitCode() {
        try (CodeBox box = new CodeBox().start()) {
            ExecOutput output = box.run(
                "import sys; sys.stderr.write('boom\\n'); sys.exit(3)"
            );

            assertFalse(output.success(), "expected non-zero exit");
            assertEquals(3, output.exitCode());
            assertTrue(output.stderr().contains("boom"));
        }
    }

    @Test
    void codeBoxCloseForceRemoveKeepsRunningMetricsAccurate() throws Exception {
        int rounds = 3;
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        Path runtimeHome = Path.of("/tmp", "bljvm-" + suffix);
        Files.createDirectories(runtimeHome);

        try {
            Options options = Options.builder()
                .homeDir(runtimeHome)
                .build();

            try (BoxliteRuntime runtime = BoxliteRuntime.create(options)) {
                for (int i = 0; i < rounds; i++) {
                    String name = "java-codebox-metrics-" + UUID.randomUUID();
                    SimpleBoxOptions simpleOptions = SimpleBoxOptions.builder()
                        .runtime(runtime)
                        .name(name)
                        .boxOptions(BoxOptions.builder().autoRemove(false).build())
                        .removeOnClose(true)
                        .build();
                    CodeBoxOptions codeOptions = CodeBoxOptions.builder()
                        .simpleBoxOptions(simpleOptions)
                        .build();

                    try (CodeBox box = new CodeBox(codeOptions).start()) {
                        assertFalse(box.id().isBlank(), "started box should have a non-blank id");
                        ExecOutput output = box.exec("sh", List.of("-lc", "echo ok"), Map.of());
                        assertTrue(output.success(), "expected shell execution success");
                    }
                }

                RuntimeMetrics metrics = runtime.metrics().join();
                assertEquals(rounds, metrics.boxesCreatedTotal());
                assertEquals(rounds, metrics.boxesStoppedTotal());
                assertEquals(0, metrics.numRunningBoxes());
                assertEquals(0, runtime.listInfo().join().size());
            }
        } finally {
            if (Files.exists(runtimeHome)) {
                try (var walk = Files.walk(runtimeHome)) {
                    walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                            // Best-effort cleanup to keep this integration test non-flaky.
                        }
                    });
                }
            }
        }
    }

    private static String runCommand(String... command) {
        try {
            Process process = new ProcessBuilder(command)
                .redirectErrorStream(true)
                .start();
            byte[] output = process.getInputStream().readAllBytes();
            int exitCode = process.waitFor();
            String text = new String(output);
            if (exitCode != 0) {
                throw new IllegalStateException(
                    "Command failed with exit code " + exitCode + ": " + String.join(" ", command) + "\n" + text
                );
            }
            return text;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Command interrupted: " + String.join(" ", command), e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run command: " + String.join(" ", command), e);
        }
    }
}
