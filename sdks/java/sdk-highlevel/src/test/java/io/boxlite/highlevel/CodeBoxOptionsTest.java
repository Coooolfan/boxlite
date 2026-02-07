package io.boxlite.highlevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.boxlite.BoxOptions;
import io.boxlite.ConfigException;
import org.junit.jupiter.api.Test;

class CodeBoxOptionsTest {
    @Test
    void defaultsUsePythonImageAndExecutables() {
        CodeBoxOptions options = CodeBoxOptions.builder().build();

        assertEquals(CodeBoxOptions.DEFAULT_IMAGE, options.simpleBoxOptions().boxOptions().image());
        assertEquals(CodeBoxOptions.DEFAULT_PYTHON_EXECUTABLE, options.pythonExecutable());
        assertEquals(CodeBoxOptions.DEFAULT_PIP_EXECUTABLE, options.pipExecutable());
    }

    @Test
    void blankPythonExecutableIsRejected() {
        ConfigException error = assertThrows(
            ConfigException.class,
            () -> CodeBoxOptions.builder().pythonExecutable(" ").build()
        );
        assertTrue(error.getMessage().contains("pythonExecutable"));
    }

    @Test
    void blankPipExecutableIsRejected() {
        ConfigException error = assertThrows(
            ConfigException.class,
            () -> CodeBoxOptions.builder().pipExecutable("").build()
        );
        assertTrue(error.getMessage().contains("pipExecutable"));
    }

    @Test
    void customSimpleBoxOptionsArePreserved() {
        SimpleBoxOptions base = SimpleBoxOptions.builder()
            .name("codebox-custom")
            .boxOptions(BoxOptions.builder().image("python:3.12-slim").build())
            .removeOnClose(false)
            .build();

        CodeBoxOptions options = CodeBoxOptions.builder()
            .simpleBoxOptions(base)
            .pythonExecutable("/usr/local/bin/python")
            .pipExecutable("/usr/local/bin/pip")
            .build();

        assertEquals(base, options.simpleBoxOptions());
        assertEquals("/usr/local/bin/python", options.pythonExecutable());
        assertEquals("/usr/local/bin/pip", options.pipExecutable());
    }
}
