package io.boxlite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** 盒子创建选项。 */
public final class BoxOptions {
    /** {@link #defaults()} 使用的默认镜像。 */
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
     * 返回默认盒子选项。
     *
     * @return 默认选项对象。
     */
    public static BoxOptions defaults() {
        return builder().build();
    }

    /**
     * 创建 {@link BoxOptions} 构建器。
     *
     * @return 构建器实例。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回 OCI 镜像引用。
     *
     * @return 镜像引用；使用 {@link #rootfsPath()} 时为 {@code null}。
     */
    public String image() {
        return image;
    }

    /**
     * 返回宿主机 rootfs 路径。
     *
     * @return rootfs 路径；使用 {@link #image()} 时为 {@code null}。
     */
    public String rootfsPath() {
        return rootfsPath;
    }

    /**
     * 返回 CPU 限制。
     *
     * @return vCPU 数量；为 {@code null} 时使用运行时默认值。
     */
    public Integer cpus() {
        return cpus;
    }

    /**
     * 返回内存限制。
     *
     * @return 内存（MiB）；为 {@code null} 时使用运行时默认值。
     */
    public Integer memoryMib() {
        return memoryMib;
    }

    /**
     * 返回磁盘大小限制。
     *
     * @return 磁盘大小（GiB）；为 {@code null} 时使用运行时默认值。
     */
    public Long diskSizeGb() {
        return diskSizeGb;
    }

    /**
     * 返回盒子内工作目录。
     *
     * @return 工作目录路径，或 {@code null}。
     */
    public String workingDir() {
        return workingDir;
    }

    /**
     * 返回进程环境变量。
     *
     * @return 环境变量映射。
     */
    public Map<String, String> env() {
        return env;
    }

    /**
     * 返回自动删除标记。
     *
     * @return {@code true}/{@code false}；为 {@code null} 时使用运行时默认值。
     */
    public Boolean autoRemove() {
        return autoRemove;
    }

    /**
     * 返回 detach 模式标记。
     *
     * @return {@code true}/{@code false}；为 {@code null} 时使用运行时默认值。
     */
    public Boolean detach() {
        return detach;
    }

    /**
     * 返回入口命令覆盖值。
     *
     * @return 入口命令参数列表。
     */
    public List<String> entrypoint() {
        return entrypoint;
    }

    /**
     * 返回命令覆盖值。
     *
     * @return 命令参数列表。
     */
    public List<String> cmd() {
        return cmd;
    }

    /**
     * 返回用户覆盖值。
     *
     * @return 用户字符串，或 {@code null}。
     */
    public String user() {
        return user;
    }

    /** {@link BoxOptions} 的构建器。 */
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
         * 设置镜像引用；当值非空白时会清空 {@code rootfsPath}。
         *
         * @param image OCI 镜像引用。
         * @return 当前构建器。
         */
        public Builder image(String image) {
            this.image = image;
            if (hasText(image)) {
                this.rootfsPath = null;
            }
            return this;
        }

        /**
         * 设置 rootfs 路径；当值非空白时会清空 {@code image}。
         *
         * @param rootfsPath 宿主机上的 rootfs 路径。
         * @return 当前构建器。
         */
        public Builder rootfsPath(String rootfsPath) {
            this.rootfsPath = rootfsPath;
            if (hasText(rootfsPath)) {
                this.image = null;
            }
            return this;
        }

        /**
         * 设置 CPU 限制。
         *
         * @param cpus vCPU 数量。
         * @return 当前构建器。
         */
        public Builder cpus(Integer cpus) {
            this.cpus = cpus;
            return this;
        }

        /**
         * 设置内存限制。
         *
         * @param memoryMib 内存（MiB）。
         * @return 当前构建器。
         */
        public Builder memoryMib(Integer memoryMib) {
            this.memoryMib = memoryMib;
            return this;
        }

        /**
         * 设置磁盘大小限制。
         *
         * @param diskSizeGb 磁盘大小（GiB）。
         * @return 当前构建器。
         */
        public Builder diskSizeGb(Long diskSizeGb) {
            this.diskSizeGb = diskSizeGb;
            return this;
        }

        /**
         * 设置盒子内工作目录。
         *
         * @param workingDir 工作目录路径。
         * @return 当前构建器。
         */
        public Builder workingDir(String workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        /**
         * 替换环境变量映射。
         *
         * @param env 环境变量映射。
         * @return 当前构建器。
         */
        public Builder env(Map<String, String> env) {
            Objects.requireNonNull(env, "env must not be null");
            this.env = new LinkedHashMap<>(env);
            return this;
        }

        /**
         * 设置单个环境变量。
         *
         * @param key 变量名。
         * @param value 变量值。
         * @return 当前构建器。
         */
        public Builder putEnv(String key, String value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            env.put(key, value);
            return this;
        }

        /**
         * 设置自动删除行为。
         *
         * @param autoRemove 是否自动删除盒子。
         * @return 当前构建器。
         */
        public Builder autoRemove(Boolean autoRemove) {
            this.autoRemove = autoRemove;
            return this;
        }

        /**
         * 设置 detach 行为。
         *
         * @param detach 创建后是否以分离状态返回。
         * @return 当前构建器。
         */
        public Builder detach(Boolean detach) {
            this.detach = detach;
            return this;
        }

        /**
         * 替换入口命令。
         *
         * @param entrypoint 入口命令参数列表。
         * @return 当前构建器。
         */
        public Builder entrypoint(List<String> entrypoint) {
            Objects.requireNonNull(entrypoint, "entrypoint must not be null");
            this.entrypoint = new ArrayList<>(entrypoint);
            return this;
        }

        /**
         * 替换命令参数。
         *
         * @param cmd 命令参数列表。
         * @return 当前构建器。
         */
        public Builder cmd(List<String> cmd) {
            Objects.requireNonNull(cmd, "cmd must not be null");
            this.cmd = new ArrayList<>(cmd);
            return this;
        }

        /**
         * 设置用户覆盖值。
         *
         * @param user 用户字符串。
         * @return 当前构建器。
         */
        public Builder user(String user) {
            this.user = user;
            return this;
        }

        /**
         * 构建不可变盒子选项。
         *
         * @return 选项对象。
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
