package io.boxlite.samples.smoke;

import io.boxlite.BoxHandle;
import io.boxlite.BoxOptions;
import io.boxlite.Boxlite;
import io.boxlite.BoxliteRuntime;
import io.boxlite.GetOrCreateResult;
import java.util.UUID;

/** Phase 1 smoke application for local verification. */
public final class SmokeApp {
    private SmokeApp() {
    }

    public static void main(String[] args) {
        System.out.println("BoxLite version: " + Boxlite.version());

        try (BoxliteRuntime runtime = Boxlite.newRuntime()) {
            System.out.println("Runtime created");

            String name = "java-smoke-" + UUID.randomUUID();
            GetOrCreateResult result = runtime.getOrCreate(BoxOptions.defaults(), name).join();
            BoxHandle box = result.box();

            System.out.println("Box id: " + box.id());
            System.out.println("Box created now: " + result.created());
            System.out.println("Boxes total: " + runtime.listInfo().join().size());

            runtime.remove(box.id(), false).join();
            box.close();
            runtime.shutdown(1).join();
        }

        System.out.println("Runtime closed");
    }
}
