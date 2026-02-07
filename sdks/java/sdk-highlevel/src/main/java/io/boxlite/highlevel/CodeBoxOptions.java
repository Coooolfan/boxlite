package io.boxlite.highlevel;

import io.boxlite.BoxOptions;
import io.boxlite.ConfigException;

/** {@link CodeBox} 的配置。 */
public final class CodeBoxOptions {
    /** Python 场景使用的默认镜像。 */
    public static final String DEFAULT_IMAGE = "python:slim";
    /** 盒子内默认 Python 可执行文件。 */
    public static final String DEFAULT_PYTHON_EXECUTABLE = "python3";
    /** 盒子内默认 pip 可执行文件。 */
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
     * 创建 {@link CodeBoxOptions} 构建器。
     *
     * @return 构建器实例。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回基础 SimpleBox 选项。
     *
     * @return SimpleBox 选项。
     */
    public SimpleBoxOptions simpleBoxOptions() {
        return simpleBoxOptions;
    }

    /**
     * 返回 Python 可执行文件名或路径。
     *
     * @return Python 可执行文件。
     */
    public String pythonExecutable() {
        return pythonExecutable;
    }

    /**
     * 返回 pip 可执行文件名或路径。
     *
     * @return pip 可执行文件。
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

    /** {@link CodeBoxOptions} 的构建器。 */
    public static final class Builder {
        private SimpleBoxOptions simpleBoxOptions;
        private String pythonExecutable = DEFAULT_PYTHON_EXECUTABLE;
        private String pipExecutable = DEFAULT_PIP_EXECUTABLE;

        private Builder() {
        }

        /**
         * 设置基础 SimpleBox 选项。
         *
         * @param simpleBoxOptions SimpleBox 选项。
         * @return 当前构建器。
         */
        public Builder simpleBoxOptions(SimpleBoxOptions simpleBoxOptions) {
            if (simpleBoxOptions == null) {
                throw new ConfigException("simpleBoxOptions must not be null");
            }
            this.simpleBoxOptions = simpleBoxOptions;
            return this;
        }

        /**
         * 设置 Python 可执行文件名或路径。
         *
         * @param pythonExecutable 可执行文件值。
         * @return 当前构建器。
         */
        public Builder pythonExecutable(String pythonExecutable) {
            this.pythonExecutable = pythonExecutable;
            return this;
        }

        /**
         * 设置 pip 可执行文件名或路径。
         *
         * @param pipExecutable 可执行文件值。
         * @return 当前构建器。
         */
        public Builder pipExecutable(String pipExecutable) {
            this.pipExecutable = pipExecutable;
            return this;
        }

        /**
         * 构建不可变 CodeBox 选项。
         *
         * @return 选项对象。
         */
        public CodeBoxOptions build() {
            return new CodeBoxOptions(this);
        }
    }
}
