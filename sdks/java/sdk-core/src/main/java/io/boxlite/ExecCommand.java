package io.boxlite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** {@link BoxHandle#exec(ExecCommand)} 的命令选项。 */
public final class ExecCommand {
    private final String command;
    private final List<String> args;
    private final Map<String, String> env;
    private final Long timeoutMillis;
    private final String workingDir;
    private final boolean tty;

    private ExecCommand(Builder builder) {
        this.command = builder.command;
        this.args = List.copyOf(builder.args);
        this.env = Map.copyOf(builder.env);
        this.timeoutMillis = builder.timeoutMillis;
        this.workingDir = builder.workingDir;
        this.tty = builder.tty;
    }

    /**
     * 创建执行命令构建器。
     *
     * @param command 可执行文件名或路径。
     * @return 构建器实例。
     */
    public static Builder builder(String command) {
        return new Builder(command);
    }

    /**
     * 返回命令可执行项。
     *
     * @return 命令名或路径。
     */
    public String command() {
        return command;
    }

    /**
     * 返回位置参数列表。
     *
     * @return 参数列表。
     */
    public List<String> args() {
        return args;
    }

    /**
     * 返回环境变量。
     *
     * @return 环境变量映射。
     */
    public Map<String, String> env() {
        return env;
    }

    /**
     * 返回可选超时时间。
     *
     * @return 超时时间（毫秒），或 {@code null}。
     */
    public Long timeoutMillis() {
        return timeoutMillis;
    }

    /**
     * 返回可选工作目录。
     *
     * @return 工作目录路径，或 {@code null}。
     */
    public String workingDir() {
        return workingDir;
    }

    /**
     * 返回 TTY 标记。
     *
     * @return 为 {@code true} 时分配 TTY。
     */
    public boolean tty() {
        return tty;
    }

    /** {@link ExecCommand} 的构建器。 */
    public static final class Builder {
        private final String command;
        private List<String> args = List.of();
        private Map<String, String> env = new LinkedHashMap<>();
        private Long timeoutMillis;
        private String workingDir;
        private boolean tty;

        private Builder(String command) {
            if (command == null || command.isBlank()) {
                throw new ConfigException("command must not be null or blank");
            }
            this.command = command;
        }

        /**
         * 替换参数列表。
         *
         * @param args 命令参数。
         * @return 当前构建器。
         */
        public Builder args(List<String> args) {
            Objects.requireNonNull(args, "args must not be null");
            this.args = new ArrayList<>(args);
            return this;
        }

        /**
         * 追加一个命令参数。
         *
         * @param arg 参数值。
         * @return 当前构建器。
         */
        public Builder addArg(String arg) {
            Objects.requireNonNull(arg, "arg must not be null");
            if (args.isEmpty()) {
                args = new ArrayList<>();
            }
            args.add(arg);
            return this;
        }

        /**
         * 替换环境变量映射。
         *
         * @param env 环境变量。
         * @return 当前构建器。
         */
        public Builder env(Map<String, String> env) {
            Objects.requireNonNull(env, "env must not be null");
            this.env = new LinkedHashMap<>(env);
            return this;
        }

        /**
         * 添加一个环境变量。
         *
         * @param key 变量名。
         * @param value 变量值。
         * @return 当前构建器。
         */
        public Builder putEnv(String key, String value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            env.put(key, value);
            return this;
        }

        /**
         * 设置超时时间。
         *
         * @param timeoutMillis 超时时间（毫秒）；提供时必须 {@code > 0}。
         * @return 当前构建器。
         */
        public Builder timeoutMillis(Long timeoutMillis) {
            if (timeoutMillis != null && timeoutMillis <= 0) {
                throw new ConfigException("timeoutMillis must be > 0 when provided");
            }
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        /**
         * 设置工作目录。
         *
         * @param workingDir 工作目录路径。
         * @return 当前构建器。
         */
        public Builder workingDir(String workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        /**
         * 开启或关闭 TTY 分配。
         *
         * @param tty TTY 标记。
         * @return 当前构建器。
         */
        public Builder tty(boolean tty) {
            this.tty = tty;
            return this;
        }

        /**
         * 构建不可变执行命令。
         *
         * @return 命令对象。
         */
        public ExecCommand build() {
            return new ExecCommand(this);
        }
    }
}
