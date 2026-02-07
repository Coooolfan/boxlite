package io.boxlite;

/** {@link BoxHandle#copyIn} 与 {@link BoxHandle#copyOut} 的复制行为选项。 */
public final class CopyOptions {
    private final boolean recursive;
    private final boolean overwrite;
    private final boolean followSymlinks;
    private final boolean includeParent;

    private CopyOptions(Builder builder) {
        this.recursive = builder.recursive;
        this.overwrite = builder.overwrite;
        this.followSymlinks = builder.followSymlinks;
        this.includeParent = builder.includeParent;
    }

    /**
     * 返回默认复制选项。
     *
     * @return 默认选项对象。
     */
    public static CopyOptions defaults() {
        return builder().build();
    }

    /**
     * 创建复制选项构建器。
     *
     * @return 构建器实例。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回递归复制标记。
     *
     * @return 为 {@code true} 时递归复制目录。
     */
    public boolean recursive() {
        return recursive;
    }

    /**
     * 返回覆盖标记。
     *
     * @return 为 {@code true} 时覆盖目标已存在文件。
     */
    public boolean overwrite() {
        return overwrite;
    }

    /**
     * 返回符号链接跟随标记。
     *
     * @return 为 {@code true} 时跟随符号链接。
     */
    public boolean followSymlinks() {
        return followSymlinks;
    }

    /**
     * 返回包含父目录标记。
     *
     * @return 为 {@code true} 时保留父目录结构。
     */
    public boolean includeParent() {
        return includeParent;
    }

    /** {@link CopyOptions} 的构建器。 */
    public static final class Builder {
        private boolean recursive = true;
        private boolean overwrite = true;
        private boolean followSymlinks = false;
        private boolean includeParent = true;

        private Builder() {
        }

        /**
         * 设置是否递归复制。
         *
         * @param recursive 递归标记。
         * @return 当前构建器。
         */
        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        /**
         * 设置是否覆盖目标文件。
         *
         * @param overwrite 覆盖标记。
         * @return 当前构建器。
         */
        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        /**
         * 设置是否跟随符号链接。
         *
         * @param followSymlinks 符号链接跟随标记。
         * @return 当前构建器。
         */
        public Builder followSymlinks(boolean followSymlinks) {
            this.followSymlinks = followSymlinks;
            return this;
        }

        /**
         * 设置是否包含父目录结构。
         *
         * @param includeParent 包含父目录标记。
         * @return 当前构建器。
         */
        public Builder includeParent(boolean includeParent) {
            this.includeParent = includeParent;
            return this;
        }

        /**
         * 构建不可变复制选项。
         *
         * @return 选项对象。
         */
        public CopyOptions build() {
            return new CopyOptions(this);
        }
    }
}
