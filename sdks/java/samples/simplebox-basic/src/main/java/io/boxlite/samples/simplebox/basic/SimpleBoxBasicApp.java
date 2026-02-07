package io.boxlite.samples.simplebox.basic;

import io.boxlite.BoxOptions;
import io.boxlite.highlevel.ExecOutput;
import io.boxlite.highlevel.SimpleBox;
import io.boxlite.highlevel.SimpleBoxOptions;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Basic SimpleBox sample: create a box and execute one command. */
public final class SimpleBoxBasicApp {
    private SimpleBoxBasicApp() {
    }

    public static void main(String[] args) {
        String name = "java-simplebox-basic-" + UUID.randomUUID();
        SimpleBoxOptions options = SimpleBoxOptions.builder()
            .name(name)
            .boxOptions(BoxOptions.defaults())
            .removeOnClose(true)
            .build();

        try (SimpleBox box = new SimpleBox(options).start()) {
            ExecOutput output = box.exec(
                "sh",
                List.of("-lc", "printf 'hello %s from %s\\n' \"$WHO\" \"$PWD\""),
                Map.of("WHO", "simplebox")
            );

            System.out.println("Box id: " + box.id());
            System.out.println("Exit code: " + output.exitCode());
            System.out.println("Stdout: " + output.stdout().strip());
            if (!output.stderr().isBlank()) {
                System.out.println("Stderr: " + output.stderr().strip());
            }
            if (output.errorMessage() != null && !output.errorMessage().isBlank()) {
                System.out.println("Error message: " + output.errorMessage());
            }
        }
    }
}
