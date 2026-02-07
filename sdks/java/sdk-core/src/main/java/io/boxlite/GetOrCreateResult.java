package io.boxlite;

import java.util.Objects;

/** Result returned by {@link BoxliteRuntime#getOrCreate}. */
public final class GetOrCreateResult {
    private final BoxHandle box;
    private final boolean created;

    public GetOrCreateResult(BoxHandle box, boolean created) {
        this.box = Objects.requireNonNull(box, "box must not be null");
        this.created = created;
    }

    public BoxHandle box() {
        return box;
    }

    public boolean created() {
        return created;
    }
}
