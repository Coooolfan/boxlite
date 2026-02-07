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

    /**
     * Returns process exit code.
     *
     * @return exit code
     */
    public int exitCode() {
        return exitCode;
    }

    /**
     * Returns captured stdout.
     *
     * @return stdout text
     */
    public String stdout() {
        return stdout;
    }

    /**
     * Returns captured stderr.
     *
     * @return stderr text
     */
    public String stderr() {
        return stderr;
    }

    /**
     * Returns optional runtime/native error message.
     *
     * @return error message, or {@code null}
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * Returns whether command exited successfully.
     *
     * @return {@code true} when exit code is zero
     */
    public boolean success() {
        return exitCode == 0;
    }
}
