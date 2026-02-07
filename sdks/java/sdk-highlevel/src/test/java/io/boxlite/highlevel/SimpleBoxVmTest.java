package io.boxlite.highlevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import io.boxlite.BoxOptions;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class SimpleBoxVmTest {
    @BeforeAll
    static void verifyVmPreflight() {
        String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String osArch = System.getProperty("os.arch", "");

        if (!osName.contains("mac")) {
            return;
        }

        if (!"aarch64".equals(osArch)) {
            fail("SimpleBox VM tests require macOS arm64. Detected os.arch=" + osArch);
        }

        String hvSupport = runCommand("sysctl", "-n", "kern.hv_support").trim();
        if (!"1".equals(hvSupport)) {
            fail("Hypervisor.framework is unavailable (kern.hv_support=" + hvSupport + ")");
        }
    }

    @Test
    void simpleBoxCanStartAndExec() {
        try (SimpleBox box = new SimpleBox(SimpleBoxOptions.builder()
            .name("java-simplebox-vm-" + UUID.randomUUID())
            .boxOptions(BoxOptions.builder().autoRemove(false).build())
            .removeOnClose(true)
            .build()).start()) {

            ExecOutput output = box.exec(
                "sh",
                List.of("-lc", "printf '%s|%s\\n' \"$BOXLITE_SIMPLEBOX\" \"$PWD\""),
                Map.of("BOXLITE_SIMPLEBOX", "ok")
            );

            assertTrue(output.success(), "expected exec success");
            assertEquals("ok|/", output.stdout().strip());
        }
    }

    @Test
    void simpleBoxCanReuseExistingByName() {
        String name = "java-simplebox-reuse-" + UUID.randomUUID();
        BoxOptions options = BoxOptions.builder().autoRemove(false).build();
        String marker = "persisted-" + UUID.randomUUID();
        String firstId;

        try (SimpleBox first = new SimpleBox(SimpleBoxOptions.builder()
            .name(name)
            .boxOptions(options)
            .removeOnClose(false)
            .build()).start()) {
            assertTrue(first.created().orElse(false));
            firstId = first.id();

            ExecOutput writeMarker = first.exec(
                "sh",
                List.of("-lc", "mkdir -p /tmp/simplebox-reuse && printf '%s' \"$MARKER\" > /tmp/simplebox-reuse/marker.txt"),
                Map.of("MARKER", marker)
            );
            assertTrue(writeMarker.success(), "first session should write marker successfully");
        }

        try (SimpleBox second = new SimpleBox(SimpleBoxOptions.builder()
            .name(name)
            .boxOptions(options)
            .reuseExisting(true)
            .removeOnClose(true)
            .build()).start()) {
            assertFalse(second.created().orElse(true));
            assertEquals(firstId, second.id(), "reuseExisting should attach to same underlying box");

            ExecOutput readMarker = second.exec(
                "sh",
                List.of("-lc", "cat /tmp/simplebox-reuse/marker.txt"),
                Map.of()
            );
            assertTrue(readMarker.success(), "second session should read marker successfully");
            assertEquals(marker, readMarker.stdout().strip(), "box state should persist between sessions");
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
