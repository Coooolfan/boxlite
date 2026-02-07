package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/** 盒子的公开元数据。 */
public final class BoxInfo {
    private final String id;
    private final String name;
    private final BoxStateInfo state;
    private final Instant createdAt;
    private final String image;
    private final int cpus;
    private final int memoryMib;

    /**
     * 可反序列化的盒子元数据模型。
     *
     * @param id 盒子 ID。
     * @param name 可选盒子名称。
     * @param state 运行状态快照。
     * @param createdAt RFC-3339 创建时间戳。
     * @param image 盒子使用的镜像。
     * @param cpus 配置的 CPU 数量。
     * @param memoryMib 配置的内存（MiB）。
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
     * 返回盒子 ID。
     *
     * @return 盒子 ID。
     */
    public String id() {
        return id;
    }

    /**
     * 返回盒子名称。
     *
     * @return 盒子名称，或 {@code null}。
     */
    public String name() {
        return name;
    }

    /**
     * 返回盒子状态。
     *
     * @return 状态快照。
     */
    public BoxStateInfo state() {
        return state;
    }

    /**
     * 返回创建时间。
     *
     * @return 创建时间戳。
     */
    public Instant createdAt() {
        return createdAt;
    }

    /**
     * 返回镜像引用。
     *
     * @return OCI 镜像引用。
     */
    public String image() {
        return image;
    }

    /**
     * 返回配置的 CPU 数量。
     *
     * @return CPU 数量。
     */
    public int cpus() {
        return cpus;
    }

    /**
     * 返回配置的内存（MiB）。
     *
     * @return 内存（MiB）。
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
