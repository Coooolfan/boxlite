package io.boxlite;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Command options for {@link BoxHandle#exec(ExecCommand)}. */
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
     * Creates an execution command builder.
     *
     * @param command executable name or path
     * @return builder instance
     */
    public static Builder builder(String command) {
        return new Builder(command);
    }

    /**
     * Returns command executable.
     *
     * @return command name or path
     */
    public String command() {
        return command;
    }

    /**
     * Returns positional arguments.
     *
     * @return arguments list
     */
    public List<String> args() {
        return args;
    }

    /**
     * Returns environment variables.
     *
     * @return environment map
     */
    public Map<String, String> env() {
        return env;
    }

    /**
     * Returns optional timeout.
     *
     * @return timeout in milliseconds, or {@code null}
     */
    public Long timeoutMillis() {
        return timeoutMillis;
    }

    /**
     * Returns optional working directory.
     *
     * @return working directory path, or {@code null}
     */
    public String workingDir() {
        return workingDir;
    }

    /**
     * Returns tty flag.
     *
     * @return {@code true} when tty should be allocated
     */
    public boolean tty() {
        return tty;
    }

    /** Builder for {@link ExecCommand}. */
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
         * Replaces argument list.
         *
         * @param args command arguments
         * @return this builder
         */
        public Builder args(List<String> args) {
            Objects.requireNonNull(args, "args must not be null");
            this.args = new ArrayList<>(args);
            return this;
        }

        /**
         * Appends one command argument.
         *
         * @param arg argument value
         * @return this builder
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
         * Replaces environment map.
         *
         * @param env environment variables
         * @return this builder
         */
        public Builder env(Map<String, String> env) {
            Objects.requireNonNull(env, "env must not be null");
            this.env = new LinkedHashMap<>(env);
            return this;
        }

        /**
         * Adds one environment variable.
         *
         * @param key variable name
         * @param value variable value
         * @return this builder
         */
        public Builder putEnv(String key, String value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            env.put(key, value);
            return this;
        }

        /**
         * Sets timeout.
         *
         * @param timeoutMillis timeout in milliseconds; must be {@code > 0} when provided
         * @return this builder
         */
        public Builder timeoutMillis(Long timeoutMillis) {
            if (timeoutMillis != null && timeoutMillis <= 0) {
                throw new ConfigException("timeoutMillis must be > 0 when provided");
            }
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        /**
         * Sets working directory.
         *
         * @param workingDir working directory path
         * @return this builder
         */
        public Builder workingDir(String workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        /**
         * Enables/disables tty allocation.
         *
         * @param tty tty flag
         * @return this builder
         */
        public Builder tty(boolean tty) {
            this.tty = tty;
            return this;
        }

        /**
         * Builds immutable execution command.
         *
         * @return command instance
         */
        public ExecCommand build() {
            return new ExecCommand(this);
        }
    }
}
