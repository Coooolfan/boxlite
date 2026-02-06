package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Runtime state metadata for a box. */
public final class BoxStateInfo {
    private final String status;
    private final boolean running;
    private final Integer pid;

    @JsonCreator
    public BoxStateInfo(
        @JsonProperty("status") String status,
        @JsonProperty("running") boolean running,
        @JsonProperty("pid") Integer pid
    ) {
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.running = running;
        this.pid = pid;
    }

    public String status() {
        return status;
    }

    public boolean running() {
        return running;
    }

    public Integer pid() {
        return pid;
    }
}
