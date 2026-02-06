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

    public static CopyOptions defaults() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean recursive() {
        return recursive;
    }

    public boolean overwrite() {
        return overwrite;
    }

    public boolean followSymlinks() {
        return followSymlinks;
    }

    public boolean includeParent() {
        return includeParent;
    }

    public static final class Builder {
        private boolean recursive = true;
        private boolean overwrite = true;
        private boolean followSymlinks = false;
        private boolean includeParent = true;

        private Builder() {
        }

        public Builder recursive(boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        public Builder overwrite(boolean overwrite) {
            this.overwrite = overwrite;
            return this;
        }

        public Builder followSymlinks(boolean followSymlinks) {
            this.followSymlinks = followSymlinks;
            return this;
        }

        public Builder includeParent(boolean includeParent) {
            this.includeParent = includeParent;
            return this;
        }

        public CopyOptions build() {
            return new CopyOptions(this);
        }
    }
}
