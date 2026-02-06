package io.boxlite.highlevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import io.boxlite.ConfigException;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CodeBoxVmTest {
    @BeforeAll
    static void verifyVmPreflight() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "");

        if (!osName.contains("mac")) {
            return;
        }

        if (!"aarch64".equals(osArch)) {
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
