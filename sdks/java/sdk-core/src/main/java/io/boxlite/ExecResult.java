package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Result for an execution created via {@link BoxHandle#exec(ExecCommand)}. */
public final class ExecResult {
    private final int exitCode;
    private final String errorMessage;

    /**
     * Deserializable execution result model.
     *
     * @param exitCode process exit code
     * @param errorMessage optional runtime error message from native layer
     */
    @JsonCreator
    public ExecResult(
        @JsonProperty("exitCode") int exitCode,
        @JsonProperty("errorMessage") String errorMessage
    ) {
        this.exitCode = exitCode;
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
     * Returns optional error message.
     *
     * @return native-side error message, or {@code null}
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * Returns whether execution completed successfully.
     *
     * @return {@code true} when {@link #exitCode()} is zero
     */
    public boolean success() {
        return exitCode == 0;
    }
}
