package io.boxlite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Box creation options. */
public final class BoxOptions {
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

    public static BoxOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public String image() {
        return image;
    }

    public String rootfsPath() {
        return rootfsPath;
    }

    public Integer cpus() {
        return cpus;
    }

    public Integer memoryMib() {
        return memoryMib;
    }

    public Long diskSizeGb() {
        return diskSizeGb;
    }

    public String workingDir() {
        return workingDir;
    }

    public Map<String, String> env() {
        return env;
    }

    public Boolean autoRemove() {
        return autoRemove;
    }

    public Boolean detach() {
        return detach;
    }

    public List<String> entrypoint() {
        return entrypoint;
    }

    public List<String> cmd() {
        return cmd;
    }

    public String user() {
        return user;
    }

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

        public Builder image(String image) {
            this.image = image;
            if (hasText(image)) {
                this.rootfsPath = null;
            }
            return this;
        }

        public Builder rootfsPath(String rootfsPath) {
            this.rootfsPath = rootfsPath;
            if (hasText(rootfsPath)) {
                this.image = null;
            }
            return this;
        }

        public Builder cpus(Integer cpus) {
            this.cpus = cpus;
            return this;
        }

        public Builder memoryMib(Integer memoryMib) {
            this.memoryMib = memoryMib;
            return this;
        }

        public Builder diskSizeGb(Long diskSizeGb) {
            this.diskSizeGb = diskSizeGb;
            return this;
        }

        public Builder workingDir(String workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        public Builder env(Map<String, String> env) {
            Objects.requireNonNull(env, "env must not be null");
            this.env = new LinkedHashMap<>(env);
            return this;
        }

        public Builder putEnv(String key, String value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            env.put(key, value);
            return this;
        }

        public Builder autoRemove(Boolean autoRemove) {
            this.autoRemove = autoRemove;
            return this;
        }

        public Builder detach(Boolean detach) {
            this.detach = detach;
            return this;
        }

        public Builder entrypoint(List<String> entrypoint) {
            Objects.requireNonNull(entrypoint, "entrypoint must not be null");
            this.entrypoint = new ArrayList<>(entrypoint);
            return this;
        }

        public Builder cmd(List<String> cmd) {
            Objects.requireNonNull(cmd, "cmd must not be null");
            this.cmd = new ArrayList<>(cmd);
            return this;
        }

        public Builder user(String user) {
            this.user = user;
            return this;
        }

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
