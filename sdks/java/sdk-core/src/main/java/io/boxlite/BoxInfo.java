package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/** Public metadata describing a box. */
public final class BoxInfo {
    private final String id;
    private final String name;
    private final BoxStateInfo state;
    private final Instant createdAt;
    private final String image;
    private final int cpus;
    private final int memoryMib;

    /**
     * Deserializable box metadata model.
     *
     * @param id box id
     * @param name optional box name
     * @param state runtime state snapshot
     * @param createdAt RFC-3339 creation timestamp
     * @param image image used by the box
     * @param cpus configured CPU count
     * @param memoryMib configured memory in MiB
     */
    @JsonCreator
    public BoxInfo(
        @JsonProperty("id") String id,
        @JsonProperty("name") String name,
        @JsonProperty("state") BoxStateInfo state,
        @JsonProperty("createdAt") String createdAt,
        @JsonProperty("image") String image,
        @JsonProperty("cpus") int cpus,
        @JsonProperty("memoryMib") int memoryMib
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.name = name;
        this.state = Objects.requireNonNull(state, "state must not be null");
        this.createdAt = parseCreatedAt(createdAt);
        this.image = Objects.requireNonNull(image, "image must not be null");
        this.cpus = cpus;
        this.memoryMib = memoryMib;
    }

    /**
     * Returns box id.
     *
     * @return box id
     */
    public String id() {
        return id;
    }

    /**
     * Returns box name.
     *
     * @return box name, or {@code null}
     */
    public String name() {
        return name;
    }

    /**
     * Returns box state.
     *
     * @return state snapshot
     */
    public BoxStateInfo state() {
        return state;
    }

    /**
     * Returns creation time.
     *
     * @return creation timestamp
     */
    public Instant createdAt() {
        return createdAt;
    }

    /**
     * Returns image reference.
     *
     * @return OCI image reference
     */
    public String image() {
        return image;
    }

    /**
     * Returns configured CPU count.
     *
     * @return CPU count
     */
    public int cpus() {
        return cpus;
    }

    /**
     * Returns configured memory in MiB.
     *
     * @return memory in MiB
     */
    public int memoryMib() {
        return memoryMib;
    }

    private static Instant parseCreatedAt(String value) {
        if (value == null || value.isBlank()) {
            throw new BoxliteException("BoxInfo.createdAt must not be null or blank");
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException e) {
            throw new BoxliteException("Invalid BoxInfo.createdAt timestamp: " + value, e);
        }
    }
}
