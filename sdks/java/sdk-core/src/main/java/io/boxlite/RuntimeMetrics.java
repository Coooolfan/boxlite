package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Snapshot of runtime-wide metrics counters. */
public final class RuntimeMetrics {
    private final long boxesCreatedTotal;
    private final long boxesFailedTotal;
    private final long boxesStoppedTotal;
    private final long numRunningBoxes;
    private final long totalCommandsExecuted;
    private final long totalExecErrors;

    /**
     * Deserializable runtime metrics model.
     *
     * @param boxesCreatedTotal total created boxes
     * @param boxesFailedTotal total failed box operations
     * @param boxesStoppedTotal total stopped boxes
     * @param numRunningBoxes current running boxes
     * @param totalCommandsExecuted total executed commands
     * @param totalExecErrors total command execution errors
     */
    @JsonCreator
    public RuntimeMetrics(
        @JsonProperty("boxesCreatedTotal") long boxesCreatedTotal,
        @JsonProperty("boxesFailedTotal") long boxesFailedTotal,
        @JsonProperty("boxesStoppedTotal") long boxesStoppedTotal,
        @JsonProperty("numRunningBoxes") long numRunningBoxes,
        @JsonProperty("totalCommandsExecuted") long totalCommandsExecuted,
        @JsonProperty("totalExecErrors") long totalExecErrors
    ) {
        this.boxesCreatedTotal = boxesCreatedTotal;
        this.boxesFailedTotal = boxesFailedTotal;
        this.boxesStoppedTotal = boxesStoppedTotal;
        this.numRunningBoxes = numRunningBoxes;
        this.totalCommandsExecuted = totalCommandsExecuted;
        this.totalExecErrors = totalExecErrors;
    }

    /**
     * Returns total created boxes since runtime start.
     *
     * @return created-box counter
     */
    public long boxesCreatedTotal() {
        return boxesCreatedTotal;
    }

    /**
     * Returns total failed box operations.
     *
     * @return failure counter
     */
    public long boxesFailedTotal() {
        return boxesFailedTotal;
    }

    /**
     * Returns total stopped boxes.
     *
     * @return stopped-box counter
     */
    public long boxesStoppedTotal() {
        return boxesStoppedTotal;
    }

    /**
     * Returns current number of running boxes.
     *
     * @return running-box count
     */
    public long numRunningBoxes() {
        return numRunningBoxes;
    }

    /**
     * Returns total executed commands.
     *
     * @return command counter
     */
    public long totalCommandsExecuted() {
        return totalCommandsExecuted;
    }

    /**
     * Returns total execution errors.
     *
     * @return execution-error counter
     */
    public long totalExecErrors() {
        return totalExecErrors;
    }
}
