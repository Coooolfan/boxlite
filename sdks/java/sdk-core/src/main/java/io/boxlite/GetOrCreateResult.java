package io.boxlite;

import java.util.Objects;

/** Result returned by {@link BoxliteRuntime#getOrCreate}. */
public final class GetOrCreateResult {
    private final BoxHandle box;
    private final boolean created;

    /**
     * Creates a get-or-create result.
     *
     * @param box returned box handle
     * @param created whether the box was newly created
     */
    public GetOrCreateResult(BoxHandle box, boolean created) {
        this.box = Objects.requireNonNull(box, "box must not be null");
        this.created = created;
    }

    /**
     * Returns box handle.
     *
     * @return box handle
     */
    public BoxHandle box() {
        return box;
    }

    /**
     * Returns creation status.
     *
     * @return {@code true} when runtime created a new box
     */
    public boolean created() {
        return created;
    }
}
