package io.boxlite;

/** Copy behavior options for {@link BoxHandle#copyIn} and {@link BoxHandle#copyOut}. */
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
     * Returns default copy options.
     *
     * @return options with default values
     */
    public static CopyOptions defaults() {
        return builder().build();
    }

    /**
     * Creates a copy options builder.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns recursive-copy flag.
     *
     * @return {@code true} to copy directories recursively
     */
    public boolean recursive() {
        return recursive;
    }

    /**
     * Returns overwrite flag.
     *
     * @return {@code true} to overwrite existing destination files
     */
    public boolean overwrite() {
        return overwrite;
    }

    /**
     * Returns symlink-follow flag.
     *
     * @return {@code true} to follow symlinks
     */
    public boolean followSymlinks() {
        return followSymlinks;
    }

    /**
     * Returns include-parent flag.
     *
     * @return {@code true} to preserve parent directory structure
     */
    public boolean includeParent() {
        return includeParent;
    }

    /** Builder for {@link CopyOptions}. */
    public static final class Builder {
        private boolean recursive = true;
        private boolean overwrite = true;
        private boolean followSymlinks = false;
        private boolean includeParent = true;

        private Builder() {
        }

        /**
         * Sets recursive copy behavior.
         *
         * @param recursive recursive flag
         * @return this builder
         */
        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        /**
         * Sets overwrite behavior.
         *
         * @param overwrite overwrite flag
         * @return this builder
         */
        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        /**
         * Sets symlink behavior.
         *
         * @param followSymlinks symlink-follow flag
         * @return this builder
         */
        public Builder followSymlinks(boolean followSymlinks) {
            this.followSymlinks = followSymlinks;
            return this;
        }

        /**
         * Sets include-parent behavior.
         *
         * @param includeParent include-parent flag
         * @return this builder
         */
        public Builder includeParent(boolean includeParent) {
            this.includeParent = includeParent;
            return this;
        }

        /**
         * Builds immutable copy options.
         *
         * @return options instance
         */
        public CopyOptions build() {
            return new CopyOptions(this);
        }
    }
}
