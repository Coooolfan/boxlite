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

    public static Builder builder(String command) {
        return new Builder(command);
    }

    public String command() {
        return command;
    }

    public List<String> args() {
        return args;
    }

    public Map<String, String> env() {
        return env;
    }

    public Long timeoutMillis() {
        return timeoutMillis;
    }

    public String workingDir() {
        return workingDir;
    }

    public boolean tty() {
        return tty;
    }

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

        public Builder args(List<String> args) {
            Objects.requireNonNull(args, "args must not be null");
            this.args = new ArrayList<>(args);
            return this;
        }

        public Builder addArg(String arg) {
            Objects.requireNonNull(arg, "arg must not be null");
            if (args.isEmpty()) {
                args = new ArrayList<>();
            }
            args.add(arg);
            return this;
        }

        public Builder env(Map<String, String> env) {
            Objects.requireNonNull(env, "env must not be null");
            this.env = new LinkedHashMap<>(env);
            return this;
        }

        public Builder putEnv(String key, String value) {
            Objects.requireNonNull(key, "key must not be null");
            Objects.requireNonNull(value, "value must not be null");
            env.put(key, value);
            return this;
        }

        public Builder timeoutMillis(Long timeoutMillis) {
            if (timeoutMillis != null && timeoutMillis <= 0) {
                throw new ConfigException("timeoutMillis must be > 0 when provided");
            }
            this.timeoutMillis = timeoutMillis;
            return this;
        }

        public Builder workingDir(String workingDir) {
            this.workingDir = workingDir;
            return this;
        }

        public Builder tty(boolean tty) {
            this.tty = tty;
            return this;
        }

        public ExecCommand build() {
            return new ExecCommand(this);
        }
    }
}
