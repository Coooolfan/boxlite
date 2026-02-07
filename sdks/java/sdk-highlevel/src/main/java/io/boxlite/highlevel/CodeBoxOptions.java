package io.boxlite.highlevel;

import io.boxlite.BoxOptions;
import io.boxlite.ConfigException;

/** Configuration for {@link CodeBox}. */
public final class CodeBoxOptions {
    public static final String DEFAULT_IMAGE = "python:slim";
    public static final String DEFAULT_PYTHON_EXECUTABLE = "python3";
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

    public static Builder builder() {
        return new Builder();
    }

    public SimpleBoxOptions simpleBoxOptions() {
        return simpleBoxOptions;
    }

    public String pythonExecutable() {
        return pythonExecutable;
    }

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

    public static final class Builder {
        private SimpleBoxOptions simpleBoxOptions;
        private String pythonExecutable = DEFAULT_PYTHON_EXECUTABLE;
        private String pipExecutable = DEFAULT_PIP_EXECUTABLE;

        private Builder() {
        }

        public Builder simpleBoxOptions(SimpleBoxOptions simpleBoxOptions) {
            if (simpleBoxOptions == null) {
                throw new ConfigException("simpleBoxOptions must not be null");
            }
            this.simpleBoxOptions = simpleBoxOptions;
            return this;
        }

        public Builder pythonExecutable(String pythonExecutable) {
            this.pythonExecutable = pythonExecutable;
            return this;
        }

        public Builder pipExecutable(String pipExecutable) {
            this.pipExecutable = pipExecutable;
            return this;
        }

        public CodeBoxOptions build() {
            return new CodeBoxOptions(this);
        }
    }
}
