package io.boxlite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class ExecCommandTest {
    @Test
    void builderMapsAllFields() {
        ExecCommand command = ExecCommand.builder("sh")
            .addArg("-lc")
            .addArg("echo hi")
            .putEnv("FOO", "bar")
            .workingDir("/workspace")
            .timeoutMillis(1_234L)
            .tty(true)
            .build();

        assertEquals("sh", command.command());
        assertEquals(2, command.args().size());
        assertEquals("bar", command.env().get("FOO"));
        assertEquals("/workspace", command.workingDir());
        assertEquals(1_234L, command.timeoutMillis());
        assertTrue(command.tty());
    }

    @Test
    void builderRejectsBlankCommand() {
        assertThrows(ConfigException.class, () -> ExecCommand.builder(" "));
    }

    @Test
    void builderRejectsNonPositiveTimeout() {
        assertThrows(
            ConfigException.class,
            () -> ExecCommand.builder("echo").timeoutMillis(0L).build()
        );
    }

    @Test
    void builderAllowsUnsetOptionalFields() {
        ExecCommand command = ExecCommand.builder("echo")
            .args(java.util.List.of("ok"))
            .env(Map.of())
            .build();

        assertEquals("echo", command.command());
        assertEquals(java.util.List.of("ok"), command.args());
        assertFalse(command.tty());
    }
}
