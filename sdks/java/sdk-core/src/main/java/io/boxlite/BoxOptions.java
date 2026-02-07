package io.boxlite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Box creation options. */
public final class BoxOptions {
    /** Default image used by {@link #defaults()}. */
    public static final String DEFAULT_IMAGE = "alpine:latest";

    private final String image;
    private final String rootfsPath;
    private final Integer cpus;
    private final Integer memoryMib;
    private final Long diskSizeGb;
    private final String workingDir;
    private final Map<String, String> env;
    private final Boolean autoRemove;
    private final Boolean detach;
    private final List<String> entrypoint;
    private final List<String> cmd;
    private final String user;

    private BoxOptions(Builder builder) {
        this.image = builder.image;
        this.rootfsPath = builder.rootfsPath;
        this.cpus = builder.cpus;
        this.memoryMib = builder.memoryMib;
        this.diskSizeGb = builder.diskSizeGb;
        this.workingDir = builder.workingDir;
        this.env = Map.copyOf(builder.env);
        this.autoRemove = builder.autoRemove;
        this.detach = builder.detach;
        this.entrypoint = List.copyOf(builder.entrypoint);
        this.cmd = List.copyOf(builder.cmd);
        this.user = builder.user;
    }

    /**
     * Returns default box options.
     *
     * @return options with default values
     */
    public static BoxOptions defaults() {
        return builder().build();
    }

    /**
     * Creates a {@link BoxOptions} builder.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns OCI image reference.
     *
     * @return image reference, or {@code null} when {@link #rootfsPath()} is used
     */
    public String image() {
        return image;
    }

    /**
     * Returns host rootfs path.
     *
     * @return rootfs path, or {@code null} when {@link #image()} is used
     */
    public String rootfsPath() {
        return rootfsPath;
    }

    /**
     * Returns CPU limit.
     *
     * @return CPU count in vCPU units, or {@code null} for runtime default
     */
    public Integer cpus() {
        return cpus;
    }

    /**
     * Returns memory limit.
     *
     * @return memory in MiB, or {@code null} for runtime default
     */
    public Integer memoryMib() {
        return memoryMib;
    }

    /**
     * Returns disk size limit.
     *
     * @return disk size in GiB, or {@code null} for runtime default
     */
    public Long diskSizeGb() {
        return diskSizeGb;
    }

    /**
     * Returns working directory inside the box.
     *
     * @return working directory path, or {@code null}
     */
    public String workingDir() {
        return workingDir;
    }

    /**
     * Returns process environment.
     *
     * @return environment map
     */
    public Map<String, String> env() {
        return env;
    }

    /**
     * Returns auto-remove flag.
     *
     * @return {@code true}/{@code false}, or {@code null} for runtime default
     */
    public Boolean autoRemove() {
        return autoRemove;
    }

    /**
     * Returns detach mode flag.
     *
     * @return {@code true}/{@code false}, or {@code null} for runtime default
     */
    public Boolean detach() {
        return detach;
    }

    /**
     * Returns entrypoint override.
     *
     * @return entrypoint command tokens
     */
    public List<String> entrypoint() {
        return entrypoint;
    }

    /**
     * Returns command override.
     *
     * @return command tokens
     */
    public List<String> cmd() {
        return cmd;
    }

    /**
     * Returns user override.
     *
     * @return user string, or {@code null}
     */
    public String user() {
        return user;
    }

    /** Builder for {@link BoxOptions}. */
    public static final class Builder {
        private String image = DEFAULT_IMAGE;
        private String rootfsPath;
        private Integer cpus;
        private Integer memoryMib;
        private Long diskSizeGb;
        private String workingDir;
        private Map<String, String> env = new LinkedHashMap<>();
        private Boolean autoRemove;
        private Boolean detach;
        private List<String> entrypoint = List.of();
        private List<String> cmd = List.of();
        private String user;

        private Builder() {
        }

        /**
         * Sets image reference and clears {@code rootfsPath} when non-blank.
         *
         * @param image OCI image reference
         * @return this builder
         */
        public Builder image(String image) {
            this.image = image;
            if (hasText(image)) {
                this.rootfsPath = null;
            }
            return this;
        }

        /**
         * Sets rootfs path and clears {@code image} when non-blank.
         *
         * @param rootfsPath host path to prepared rootfs
         * @return this builder
         */
        public Builder rootfsPath(String rootfsPath) {
            this.rootfsPath = rootfsPath;
            if (hasText(rootfsPath)) {
                this.image = null;
            }
            return this;
        }

        /**
         * Sets CPU limit.
         *
         * @param cpus vCPU count
         * @return this builder
         */
        public Builder cpus(Integer cpus) {
            this.cpus = cpus;
            return this;
        }

        /**
         * Sets memory limit.
         *
         * @param memoryMib memory in MiB
         * @return this builder
         */
        public Builder memoryMib(Integer memoryMib) {
            this.memoryMib = memoryMib;
            return this;
        }

        /**
         * Sets disk size limit.
         *
         * @param diskSizeGb disk size in GiB
         * @return this builder
         */
        public Builder diskSizeGb(Long diskSizeGb) {
            this.diskSizeGb = diskSizeGb;
            return this;
        }

        /**
         * Sets working directory inside box.
         *
         * @param workingDir working directory path
         * @return this builder
         */
        public Builder workingDir(String workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        /**
         * Replaces environment map.
         *
         * @param env environment map
         * @return this builder
         */
        public Builder env(Map<String, String> env) {
            Objects.requireNonNull(env, "env must not be null");
            this.env = new LinkedHashMap<>(env);
            return this;
        }

        /**
         * Sets one environment variable.
         *
         * @param key variable name
         * @param value variable value
         * @return this builder
         */
        public Builder putEnv(String key, String value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            env.put(key, value);
            return this;
        }

        /**
         * Sets auto-remove behavior.
         *
         * @param autoRemove whether box should be removed automatically
         * @return this builder
         */
        public Builder autoRemove(Boolean autoRemove) {
            this.autoRemove = autoRemove;
            return this;
        }

        /**
         * Sets detach behavior.
         *
         * @param detach whether creation returns detached state
         * @return this builder
         */
        public Builder detach(Boolean detach) {
            this.detach = detach;
            return this;
        }

        /**
         * Replaces entrypoint command.
         *
         * @param entrypoint entrypoint tokens
         * @return this builder
         */
        public Builder entrypoint(List<String> entrypoint) {
            Objects.requireNonNull(entrypoint, "entrypoint must not be null");
            this.entrypoint = new ArrayList<>(entrypoint);
            return this;
        }

        /**
         * Replaces command arguments.
         *
         * @param cmd command tokens
         * @return this builder
         */
        public Builder cmd(List<String> cmd) {
            Objects.requireNonNull(cmd, "cmd must not be null");
            this.cmd = new ArrayList<>(cmd);
            return this;
        }

        /**
         * Sets user override.
         *
         * @param user user string
         * @return this builder
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * Builds immutable box options.
         *
         * @return options instance
         */
        public BoxOptions build() {
            validateRootfs();
            return new BoxOptions(this);
        }

        private void validateRootfs() {
            boolean hasImage = hasText(image);
            boolean hasRootfsPath = hasText(rootfsPath);
            if (hasImage == hasRootfsPath) {
                throw new ConfigException(
                    "BoxOptions requires exactly one of image or rootfsPath"
                );
            }
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
