package io.boxlite.highlevel;

import io.boxlite.BoxOptions;
import io.boxlite.BoxliteRuntime;
import io.boxlite.ConfigException;

/** Configuration for {@link SimpleBox}. */
public final class SimpleBoxOptions {
    private final BoxliteRuntime runtime;
    private final BoxOptions boxOptions;
    private final String name;
    private final boolean reuseExisting;
    private final boolean removeOnClose;

    private SimpleBoxOptions(Builder builder) {
        this.runtime = builder.runtime;
        this.boxOptions = builder.boxOptions;
        this.name = builder.name;
        this.reuseExisting = builder.reuseExisting;
        this.removeOnClose = builder.removeOnClose;
    }

    /**
     * Creates a builder for {@link SimpleBoxOptions}.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns runtime to use.
     *
     * @return runtime instance, or {@code null} to auto-create one
     */
    public BoxliteRuntime runtime() {
        return runtime;
    }

    /**
     * Returns low-level box options.
     *
     * @return box options
     */
    public BoxOptions boxOptions() {
        return boxOptions;
    }

    /**
     * Returns optional box name.
     *
     * @return box name, or {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns reuse-existing flag.
     *
     * @return {@code true} to reuse existing box by name
     */
    public boolean reuseExisting() {
        return reuseExisting;
    }

    /**
     * Returns remove-on-close flag.
     *
     * @return {@code true} to remove box when wrapper is closed
     */
    public boolean removeOnClose() {
        return removeOnClose;
    }

    /** Builder for {@link SimpleBoxOptions}. */
    public static final class Builder {
        private BoxliteRuntime runtime;
        private BoxOptions boxOptions = BoxOptions.defaults();
        private String name;
        private boolean reuseExisting;
        private boolean removeOnClose = true;

        private Builder() {
        }

        /**
         * Sets runtime instance.
         *
         * @param runtime runtime instance
         * @return this builder
         */
        public Builder runtime(BoxliteRuntime runtime) {
            this.runtime = runtime;
            return this;
        }

        /**
         * Sets low-level box options.
         *
         * @param boxOptions box options
         * @return this builder
         */
        public Builder boxOptions(BoxOptions boxOptions) {
            if (boxOptions == null) {
                throw new ConfigException("boxOptions must not be null");
            }
            this.boxOptions = boxOptions;
            return this;
        }

        /**
         * Sets box name.
         *
         * @param name box name
         * @return this builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * Sets reuse behavior.
         *
         * @param reuseExisting whether to attach existing box by name
         * @return this builder
         */
        public Builder reuseExisting(boolean reuseExisting) {
            this.reuseExisting = reuseExisting;
            return this;
        }

        /**
         * Sets remove-on-close behavior.
         *
         * @param removeOnClose whether close should remove the box
         * @return this builder
         */
        public Builder removeOnClose(boolean removeOnClose) {
            this.removeOnClose = removeOnClose;
            return this;
        }

        /**
         * Builds immutable simple-box options.
         *
         * @return options instance
         */
        public SimpleBoxOptions build() {
            if (reuseExisting && (name == null || name.isBlank())) {
                throw new ConfigException("name must not be blank when reuseExisting=true");
            }
            return new SimpleBoxOptions(this);
        }
    }
}
