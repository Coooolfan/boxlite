package io.boxlite;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class BoxliteSmokeTest {
    @Test
    void versionIsAvailable() {
        String version = Boxlite.version();
        assertFalse(version.isBlank(), "Version should not be blank");
    }

    @Test
    void runtimeCanBeCreatedAndClosed() {
        assertDoesNotThrow(() -> {
            try (BoxliteRuntime ignored = Boxlite.newRuntime()) {
                // no-op
            }
        });
    }
}
