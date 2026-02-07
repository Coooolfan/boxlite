package io.boxlite.highlevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.boxlite.ConfigException;
import org.junit.jupiter.api.Test;

class SimpleBoxOptionsTest {
    @Test
    void defaultsAreSafe() {
        SimpleBoxOptions options = SimpleBoxOptions.builder().build();

        assertFalse(options.reuseExisting());
        assertTrue(options.removeOnClose());
    }

    @Test
    void reuseExistingRequiresName() {
        ConfigException error = assertThrows(
            ConfigException.class,
            () -> SimpleBoxOptions.builder().reuseExisting(true).build()
        );

        assertEquals("name must not be blank when reuseExisting=true", error.getMessage());
    }

    @Test
    void acceptsReuseExistingWhenNameProvided() {
        SimpleBoxOptions options = SimpleBoxOptions.builder()
            .name("simple-box")
            .reuseExisting(true)
            .removeOnClose(false)
            .build();

        assertTrue(options.reuseExisting());
        assertEquals("simple-box", options.name());
        assertFalse(options.removeOnClose());
    }
}
