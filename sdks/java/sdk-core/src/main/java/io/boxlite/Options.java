package io.boxlite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Runtime options for creating {@link BoxliteRuntime} instances. */
public final class Options {
    private final Path homeDir;
    private final List<String> imageRegistries;

    private Options(Builder builder) {
        this.homeDir = builder.homeDir;
        this.imageRegistries = List.copyOf(builder.imageRegistries);
    }

    public static Options defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Path homeDir() {
        return homeDir;
    }

    public List<String> imageRegistries() {
        return imageRegistries;
    }

    public static final class Builder {
        private Path homeDir;
        private List<String> imageRegistries = List.of();

        private Builder() {
        }

        public Builder homeDir(Path homeDir) {
            this.homeDir = homeDir;
            return this;
        }

        public Builder imageRegistries(List<String> imageRegistries) {
            Objects.requireNonNull(imageRegistries, "imageRegistries must not be null");
            this.imageRegistries = new ArrayList<>(imageRegistries);
            return this;
        }

        public Builder addImageRegistry(String imageRegistry) {
            Objects.requireNonNull(imageRegistry, "imageRegistry must not be null");
            if (imageRegistries.isEmpty()) {
                imageRegistries = new ArrayList<>();
            }
            imageRegistries.add(imageRegistry);
            return this;
        }

        public Options build() {
            return new Options(this);
        }
    }
}
