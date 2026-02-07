package io.boxlite.samples.smoke;

import io.boxlite.*;

import java.util.UUID;

/**
 * Phase 2 smoke application for local verification.
 */
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

            ExecutionHandle execEcho = box.exec(
                    ExecCommand.builder("sh")
                            .addArg("-lc")
                            .addArg("echo hello-from-boxlite-java")
                            .build()
            ).join();

            String lineEcho = execEcho.stdoutNextLine().join().orElse("<no stdout>");
            ExecResult execEchoResult = execEcho.waitFor().join();
            System.out.println("Exec stdout: " + lineEcho.strip());
            System.out.println("Exec exit code: " + execEchoResult.exitCode());

            execEcho.close();

            runtime.remove(box.id(), true).join();
            box.close();
            runtime.shutdown(1).join();
        }

        System.out.println("Runtime closed");
    }
}
