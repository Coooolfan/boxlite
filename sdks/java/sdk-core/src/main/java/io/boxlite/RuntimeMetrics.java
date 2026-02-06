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

    public long boxesCreatedTotal() {
        return boxesCreatedTotal;
    }

    public long boxesFailedTotal() {
        return boxesFailedTotal;
    }

    public long boxesStoppedTotal() {
        return boxesStoppedTotal;
    }

    public long numRunningBoxes() {
        return numRunningBoxes;
    }

    public long totalCommandsExecuted() {
        return totalCommandsExecuted;
    }

    public long totalExecErrors() {
        return totalExecErrors;
    }
}
