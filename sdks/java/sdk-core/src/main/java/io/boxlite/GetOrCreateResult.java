package io.boxlite;

import java.util.Objects;

/** {@link BoxliteRuntime#getOrCreate} 的返回结果。 */
public final class GetOrCreateResult {
    private final BoxHandle box;
    private final boolean created;

    /**
     * 创建 get-or-create 结果对象。
     *
     * @param box 返回的盒子句柄。
     * @param created 盒子是否为新创建。
     */
    public GetOrCreateResult(BoxHandle box, boolean created) {
        this.box = Objects.requireNonNull(box, "box must not be null");
        this.created = created;
    }

    /**
     * 返回盒子句柄。
     *
     * @return 盒子句柄。
     */
    public BoxHandle box() {
        return box;
    }

    /**
     * 返回创建状态。
     *
     * @return 运行时新建盒子时为 {@code true}。
     */
    public boolean created() {
        return created;
    }
}
