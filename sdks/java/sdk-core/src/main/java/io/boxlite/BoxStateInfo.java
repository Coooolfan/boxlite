package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** Runtime state metadata for a box. */
public final class BoxStateInfo {
    private final String status;
    private final boolean running;
    private final Integer pid;

    /**
     * Deserializable box state model.
     *
     * @param status status text
     * @param running whether box is currently running
     * @param pid optional process id
     */
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

    /**
     * Returns status text.
     *
     * @return state label
     */
    public String status() {
        return status;
    }

    /**
     * Returns whether box is running.
     *
     * @return running flag
     */
    public boolean running() {
        return running;
    }

    /**
     * Returns box process id.
     *
     * @return pid when available, otherwise {@code null}
     */
    public Integer pid() {
        return pid;
    }
}
