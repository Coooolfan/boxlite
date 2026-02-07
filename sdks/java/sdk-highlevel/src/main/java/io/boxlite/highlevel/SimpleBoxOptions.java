package io.boxlite.highlevel;

import io.boxlite.BoxOptions;
import io.boxlite.BoxliteRuntime;
import io.boxlite.ConfigException;

/** {@link SimpleBox} 的配置。 */
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
     * 创建 {@link SimpleBoxOptions} 构建器。
     *
     * @return 构建器实例。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * 返回要使用的运行时。
     *
     * @return 运行时实例；为 {@code null} 时使用进程级默认运行时。
     */
    public BoxliteRuntime runtime() {
        return runtime;
    }

    /**
     * 返回底层盒子选项。
     *
     * @return 盒子选项。
     */
    public BoxOptions boxOptions() {
        return boxOptions;
    }

    /**
     * 返回可选盒子名称。
     *
     * @return 盒子名称，或 {@code null}。
     */
    public String name() {
        return name;
    }

    /**
     * 返回复用已存在盒子标记。
     *
     * @return 为 {@code true} 时按名称复用已存在盒子。
     */
    public boolean reuseExisting() {
        return reuseExisting;
    }

    /**
     * 返回关闭时删除标记。
     *
     * @return 为 {@code true} 时在封装对象关闭时删除盒子。
     */
    public boolean removeOnClose() {
        return removeOnClose;
    }

    /** {@link SimpleBoxOptions} 的构建器。 */
    public static final class Builder {
        private BoxliteRuntime runtime;
        private BoxOptions boxOptions = BoxOptions.defaults();
        private String name;
        private boolean reuseExisting;
        private boolean removeOnClose = true;

        private Builder() {
        }

        /**
         * 设置运行时实例。
         *
         * @param runtime 运行时实例；传 {@code null} 时使用进程级默认运行时。
         * @return 当前构建器。
         */
        public Builder runtime(BoxliteRuntime runtime) {
            this.runtime = runtime;
            return this;
        }

        /**
         * 设置底层盒子选项。
         *
         * @param boxOptions 盒子选项。
         * @return 当前构建器。
         */
        public Builder boxOptions(BoxOptions boxOptions) {
            if (boxOptions == null) {
                throw new ConfigException("boxOptions must not be null");
            }
            this.boxOptions = boxOptions;
            return this;
        }

        /**
         * 设置盒子名称。
         *
         * @param name 盒子名称。
         * @return 当前构建器。
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * 设置复用行为。
         *
         * @param reuseExisting 是否按名称附加到已存在盒子。
         * @return 当前构建器。
         */
        public Builder reuseExisting(boolean reuseExisting) {
            this.reuseExisting = reuseExisting;
            return this;
        }

        /**
         * 设置关闭时删除行为。
         *
         * @param removeOnClose 关闭时是否删除盒子。
         * @return 当前构建器。
         */
        public Builder removeOnClose(boolean removeOnClose) {
            this.removeOnClose = removeOnClose;
            return this;
        }

        /**
         * 构建不可变 SimpleBox 选项。
         *
         * @return 选项对象。
         */
        public SimpleBoxOptions build() {
            if (reuseExisting && (name == null || name.isBlank())) {
                throw new ConfigException("name must not be blank when reuseExisting=true");
            }
            return new SimpleBoxOptions(this);
        }
    }
}
