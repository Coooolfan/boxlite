package io.boxlite;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 用于创建 {@link BoxliteRuntime} 的运行时选项。 */
public final class Options {
    private final Path homeDir;
    private final List<String> imageRegistries;

    private Options(Builder builder) {
        this.homeDir = builder.homeDir;
        this.imageRegistries = List.copyOf(builder.imageRegistries);
    }

    /**
     * 返回默认运行时选项。
     *
     * @return 默认选项对象。
     */
    public static Options defaults() {
        return builder().build();
    }

    /**
     * 创建选项构建器。
     *
     * @return 构建器实例。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回运行时主目录。
     *
     * @return 自定义主目录；为 {@code null} 时使用运行时默认目录。
     */
    public Path homeDir() {
        return homeDir;
    }

    /**
     * 返回拉取镜像时使用的有序仓库列表。
     *
     * @return 镜像仓库地址列表。
     */
    public List<String> imageRegistries() {
        return imageRegistries;
    }

    /** {@link Options} 的构建器。 */
    public static final class Builder {
        private Path homeDir;
        private List<String> imageRegistries = List.of();

        private Builder() {
        }

        /**
         * 设置运行时主目录。
         *
         * @param homeDir 运行时数据目录路径。
         * @return 当前构建器。
         */
        public Builder homeDir(Path homeDir) {
            this.homeDir = homeDir;
            return this;
        }

        /**
         * 替换镜像仓库列表。
         *
         * @param imageRegistries 镜像仓库地址列表。
         * @return 当前构建器。
         */
        public Builder imageRegistries(List<String> imageRegistries) {
            Objects.requireNonNull(imageRegistries, "imageRegistries must not be null");
            this.imageRegistries = new ArrayList<>(imageRegistries);
            return this;
        }

        /**
         * 追加一个镜像仓库地址。
         *
         * @param imageRegistry 镜像仓库地址。
         * @return 当前构建器。
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
         * 构建不可变运行时选项。
         *
         * @return 选项对象。
         */
        public Options build() {
            return new Options(this);
        }
    }
}
