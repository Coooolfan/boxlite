package io.boxlite.highlevel;

/** Aggregated result from high-level command execution. */
public final class ExecOutput {
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final String errorMessage;

    ExecOutput(int exitCode, String stdout, String stderr, String errorMessage) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
        this.errorMessage = errorMessage;
    }

    public int exitCode() {
        return exitCode;
    }

    public String stdout() {
        return stdout;
    }

    public String stderr() {
        return stderr;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public boolean success() {
        return exitCode == 0;
    }
}
