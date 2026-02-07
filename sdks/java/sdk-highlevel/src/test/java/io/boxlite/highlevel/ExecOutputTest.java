package io.boxlite.highlevel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ExecOutputTest {
    @Test
    void successTracksExitCode() {
        ExecOutput ok = new ExecOutput(0, "stdout", "", null);
        ExecOutput failed = new ExecOutput(2, "", "err", "boom");

        assertTrue(ok.success());
        assertFalse(failed.success());
        assertEquals("boom", failed.errorMessage());
    }
}
