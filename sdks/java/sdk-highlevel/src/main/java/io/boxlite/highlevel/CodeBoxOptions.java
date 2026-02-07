package io.boxlite.highlevel;

import io.boxlite.BoxOptions;
import io.boxlite.ConfigException;

/** Configuration for {@link CodeBox}. */
public final class CodeBoxOptions {
    /** Default image for Python workloads. */
    public static final String DEFAULT_IMAGE = "python:slim";
    /** Default Python executable inside the box. */
    public static final String DEFAULT_PYTHON_EXECUTABLE = "python3";
    /** Default pip executable inside the box. */
    public static final String DEFAULT_PIP_EXECUTABLE = "pip";

    private final SimpleBoxOptions simpleBoxOptions;
    private final String pythonExecutable;
    private final String pipExecutable;

    private CodeBoxOptions(Builder builder) {
        this.simpleBoxOptions = builder.simpleBoxOptions == null
            ? defaultSimpleBoxOptions()
            : builder.simpleBoxOptions;
        this.pythonExecutable = requireExecutable(builder.pythonExecutable, "pythonExecutable");
        this.pipExecutable = requireExecutable(builder.pipExecutable, "pipExecutable");
    }

    /**
     * Creates a builder for {@link CodeBoxOptions}.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Returns base simple-box options.
     *
     * @return simple-box options
     */
    public SimpleBoxOptions simpleBoxOptions() {
        return simpleBoxOptions;
    }

    /**
     * Returns Python executable name/path.
     *
     * @return python executable
     */
    public String pythonExecutable() {
        return pythonExecutable;
    }

    /**
     * Returns pip executable name/path.
     *
     * @return pip executable
     */
    public String pipExecutable() {
        return pipExecutable;
    }

    private static SimpleBoxOptions defaultSimpleBoxOptions() {
        return SimpleBoxOptions.builder()
            .boxOptions(BoxOptions.builder().image(DEFAULT_IMAGE).build())
            .build();
    }

    private static String requireExecutable(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ConfigException(fieldName + " must not be null or blank");
        }
        return value;
    }

    /** Builder for {@link CodeBoxOptions}. */
    public static final class Builder {
        private SimpleBoxOptions simpleBoxOptions;
        private String pythonExecutable = DEFAULT_PYTHON_EXECUTABLE;
        private String pipExecutable = DEFAULT_PIP_EXECUTABLE;

        private Builder() {
        }

        /**
         * Sets base simple-box options.
         *
         * @param simpleBoxOptions simple-box options
         * @return this builder
         */
        public Builder simpleBoxOptions(SimpleBoxOptions simpleBoxOptions) {
            if (simpleBoxOptions == null) {
                throw new ConfigException("simpleBoxOptions must not be null");
            }
            this.simpleBoxOptions = simpleBoxOptions;
            return this;
        }

        /**
         * Sets Python executable name/path.
         *
         * @param pythonExecutable executable value
         * @return this builder
         */
        public Builder pythonExecutable(String pythonExecutable) {
            this.pythonExecutable = pythonExecutable;
            return this;
        }

        /**
         * Sets pip executable name/path.
         *
         * @param pipExecutable executable value
         * @return this builder
         */
        public Builder pipExecutable(String pipExecutable) {
            this.pipExecutable = pipExecutable;
            return this;
        }

        /**
         * Builds immutable code-box options.
         *
         * @return options instance
         */
        public CodeBoxOptions build() {
            return new CodeBoxOptions(this);
        }
    }
}
