package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Result for an execution created via {@link BoxHandle#exec(ExecCommand)}. */
public final class ExecResult {
    private final int exitCode;
    private final String errorMessage;

    @JsonCreator
    public ExecResult(
        @JsonProperty("exitCode") int exitCode,
        @JsonProperty("errorMessage") String errorMessage
    ) {
        this.exitCode = exitCode;
        this.errorMessage = errorMessage;
    }

    public int exitCode() {
        return exitCode;
    }

    public String errorMessage() {
        return errorMessage;
    }

    public boolean success() {
        return exitCode == 0;
    }
}
