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

    public static Builder builder() {
        return new Builder();
    }

    public BoxliteRuntime runtime() {
        return runtime;
    }

    public BoxOptions boxOptions() {
        return boxOptions;
    }

    public String name() {
        return name;
    }

    public boolean reuseExisting() {
        return reuseExisting;
    }

    public boolean removeOnClose() {
        return removeOnClose;
    }

    public static final class Builder {
        private BoxliteRuntime runtime;
        private BoxOptions boxOptions = BoxOptions.defaults();
        private String name;
        private boolean reuseExisting;
        private boolean removeOnClose = true;

        private Builder() {
        }

        public Builder runtime(BoxliteRuntime runtime) {
            this.runtime = runtime;
            return this;
        }

        public Builder boxOptions(BoxOptions boxOptions) {
            if (boxOptions == null) {
                throw new ConfigException("boxOptions must not be null");
            }
            this.boxOptions = boxOptions;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder reuseExisting(boolean reuseExisting) {
            this.reuseExisting = reuseExisting;
            return this;
        }

        public Builder removeOnClose(boolean removeOnClose) {
            this.removeOnClose = removeOnClose;
            return this;
        }

        public SimpleBoxOptions build() {
            if (reuseExisting && (name == null || name.isBlank())) {
                throw new ConfigException("name must not be blank when reuseExisting=true");
            }
            return new SimpleBoxOptions(this);
        }
    }
}
