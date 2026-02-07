package io.boxlite.samples.simplebox.reuse;

import io.boxlite.BoxOptions;
import io.boxlite.highlevel.ExecOutput;
import io.boxlite.highlevel.SimpleBox;
import io.boxlite.highlevel.SimpleBoxOptions;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** SimpleBox sample: reuse an existing named box across two sessions. */
public final class SimpleBoxReuseApp {
    private SimpleBoxReuseApp() {
    }

    public static void main(String[] args) {
        String name = "java-simplebox-reuse-" + UUID.randomUUID();
        BoxOptions options = BoxOptions.builder().autoRemove(false).build();
        String firstId;
        String marker = "persisted-" + UUID.randomUUID();

        try (SimpleBox first = new SimpleBox(SimpleBoxOptions.builder()
            .name(name)
            .boxOptions(options)
            .removeOnClose(false)
            .build()).start()) {
            firstId = first.id();
            System.out.println("First session created: " + first.created().orElse(null));
            System.out.println("First session id: " + firstId);
            ExecOutput out = first.exec("sh", java.util.List.of("-lc", "echo first-session"), java.util.Map.of());
            System.out.println(out.stdout().strip());
            ExecOutput writeMarker = first.exec(
                "sh",
                List.of("-lc", "mkdir -p /tmp/simplebox-reuse && printf '%s' \"$MARKER\" > /tmp/simplebox-reuse/marker.txt"),
                Map.of("MARKER", marker)
            );
            if (!writeMarker.success()) {
                throw new IllegalStateException("failed to write marker: " + writeMarker.stderr());
            }
        }

        try (SimpleBox second = new SimpleBox(SimpleBoxOptions.builder()
            .name(name)
            .boxOptions(options)
            .reuseExisting(true)
            .removeOnClose(true)
            .build()).start()) {
            String secondId = second.id();
            System.out.println("Second session created: " + second.created().orElse(null));
            System.out.println("Second session id: " + secondId);
            System.out.println("Same box id: " + firstId.equals(secondId));
            ExecOutput out = second.exec("sh", java.util.List.of("-lc", "echo reused-session"), java.util.Map.of());
            System.out.println(out.stdout().strip());
            ExecOutput readMarker = second.exec(
                "sh",
                List.of("-lc", "cat /tmp/simplebox-reuse/marker.txt"),
                Map.of()
            );
            System.out.println("Read marker: " + readMarker.stdout().strip());
            System.out.println("Marker matches: " + marker.equals(readMarker.stdout().strip()));
        }
    }
}
