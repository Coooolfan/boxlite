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

    /**
     * Returns default runtime options.
     *
     * @return options with default values
     */
    public static Options defaults() {
        return builder().build();
    }

    /**
     * Creates an options builder.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns runtime home directory.
     *
     * @return custom home directory, or {@code null} to use runtime default
     */
    public Path homeDir() {
        return homeDir;
    }

    /**
     * Returns ordered image registry list used for pulls.
     *
     * @return registry endpoints
     */
    public List<String> imageRegistries() {
        return imageRegistries;
    }

    /** Builder for {@link Options}. */
    public static final class Builder {
        private Path homeDir;
        private List<String> imageRegistries = List.of();

        private Builder() {
        }

        /**
         * Sets runtime home directory.
         *
         * @param homeDir path for runtime data
         * @return this builder
         */
        public Builder homeDir(Path homeDir) {
            this.homeDir = homeDir;
            return this;
        }

        /**
         * Replaces image registries list.
         *
         * @param imageRegistries registry endpoints
         * @return this builder
         */
        public Builder imageRegistries(List<String> imageRegistries) {
            Objects.requireNonNull(imageRegistries, "imageRegistries must not be null");
            this.imageRegistries = new ArrayList<>(imageRegistries);
            return this;
        }

        /**
         * Appends one image registry endpoint.
         *
         * @param imageRegistry registry endpoint
         * @return this builder
         */
        public Builder addImageRegistry(String imageRegistry) {
            Objects.requireNonNull(imageRegistry, "imageRegistry must not be null");
            if (imageRegistries.isEmpty()) {
                imageRegistries = new ArrayList<>();
            }
            imageRegistries.add(imageRegistry);
            return this;
        }

        /**
         * Builds immutable runtime options.
         *
         * @return options instance
         */
        public Options build() {
            return new Options(this);
        }
    }
}
