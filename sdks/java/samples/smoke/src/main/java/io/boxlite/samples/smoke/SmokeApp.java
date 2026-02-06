package io.boxlite.samples.smoke;

import io.boxlite.Boxlite;
import io.boxlite.BoxliteRuntime;

/** Phase 0 smoke application for local verification. */
public final class SmokeApp {
    private SmokeApp() {
    }

    public static void main(String[] args) {
        System.out.println("BoxLite version: " + Boxlite.version());

        try (BoxliteRuntime ignored = Boxlite.newRuntime()) {
            System.out.println("Runtime created");
        }

        System.out.println("Runtime closed");
    }
}
