package io.boxlite.highlevel;

import io.boxlite.BoxHandle;
import io.boxlite.Boxlite;
import io.boxlite.BoxliteRuntime;
import io.boxlite.ConfigException;
import io.boxlite.ExecCommand;
import io.boxlite.ExecResult;
import io.boxlite.ExecutionHandle;
import io.boxlite.GetOrCreateResult;
import io.boxlite.NotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * High-level box wrapper for common command-execution workflows.
 *
 * <p>The class owns box lifecycle and can optionally own runtime lifecycle.
 */
public final class SimpleBox implements AutoCloseable {
    private static final ExecutorService READ_EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();

    private final BoxliteRuntime runtime;
    private final boolean ownsRuntime;
    private final SimpleBoxOptions options;

    private volatile BoxHandle box;
    private volatile Boolean created;
    private volatile boolean closed;

    public SimpleBox(SimpleBoxOptions options) {
        if (options == null) {
            throw new ConfigException("options must not be null");
        }
        this.options = options;
        if (options.runtime() == null) {
            this.runtime = Boxlite.newRuntime();
            this.ownsRuntime = true;
        } else {
            this.runtime = options.runtime();
            this.ownsRuntime = false;
        }
    }

    public synchronized SimpleBox start() {
        ensureOpen();
        if (box != null) {
            return this;
        }

        if (options.reuseExisting()) {
            GetOrCreateResult result = runtime.getOrCreate(options.boxOptions(), options.name()).join();
            box = result.box();
            created = result.created();
            return this;
        }

        box = runtime.create(options.boxOptions(), options.name()).join();
        created = true;
        return this;
    }

    public String id() {
        return requireStarted().id();
    }

    public Optional<Boolean> created() {
        return Optional.ofNullable(created);
    }

    public BoxHandle rawBox() {
        return requireStarted();
    }

    public ExecOutput exec(String command) {
        return exec(command, List.of(), Map.of());
    }

    public ExecOutput exec(String command, List<String> args, Map<String, String> env) {
        BoxHandle currentBox = requireStarted();
        if (command == null || command.isBlank()) {
            throw new ConfigException("command must not be null or blank");
        }
        List<String> resolvedArgs = args == null ? List.of() : List.copyOf(args);
        Map<String, String> resolvedEnv = env == null ? Map.of() : Map.copyOf(env);

        ExecCommand.Builder builder = ExecCommand.builder(command).args(resolvedArgs).env(resolvedEnv);
        ExecutionHandle execution = currentBox.exec(builder.build()).join();

        try {
            CompletableFuture<String> stdoutFuture = CompletableFuture.supplyAsync(
                () -> drainLines(execution, true),
                READ_EXECUTOR
            );
            CompletableFuture<String> stderrFuture = CompletableFuture.supplyAsync(
                () -> drainLines(execution, false),
                READ_EXECUTOR
            );
            ExecResult result = execution.waitFor().join();
            return new ExecOutput(
                result.exitCode(),
                stdoutFuture.join(),
                stderrFuture.join(),
                result.errorMessage()
            );
        } finally {
            execution.close();
        }
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;

        if (box != null) {
            try {
                if (options.removeOnClose()) {
                    runtime.remove(box.id(), true).join();
                }
            } catch (NotFoundException ignored) {
                // Box may already be removed by caller.
            } finally {
                box.close();
                box = null;
            }
        }

        if (ownsRuntime) {
            runtime.close();
        }
    }

    private BoxHandle requireStarted() {
        ensureOpen();
        BoxHandle currentBox = box;
        if (currentBox == null) {
            throw new ConfigException("SimpleBox is not started. Call start() first.");
        }
        return currentBox;
    }

    private void ensureOpen() {
        if (closed) {
            throw new ConfigException("SimpleBox is closed");
        }
    }

    private static String drainLines(ExecutionHandle execution, boolean stdout) {
        StringBuilder builder = new StringBuilder();
        while (true) {
            Optional<String> next = stdout
                ? execution.stdoutNextLine().join()
                : execution.stderrNextLine().join();
            if (next.isEmpty()) {
                break;
            }
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(next.get());
        }
        return builder.toString();
    }
}
