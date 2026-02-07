package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/** 盒子的运行状态元数据。 */
public final class BoxStateInfo {
    private final String status;
    private final boolean running;
    private final Integer pid;

    /**
     * 可反序列化的盒子状态模型。
     *
     * @param status 状态文本。
     * @param running 盒子当前是否运行中。
     * @param pid 可选进程 ID。
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
     * 返回状态文本。
     *
     * @return 状态标签。
     */
    public String status() {
        return status;
    }

    /**
     * 返回盒子是否运行中。
     *
     * @return 运行标记。
     */
    public boolean running() {
        return running;
    }

    /**
     * 返回盒子进程 ID。
     *
     * @return 可用时返回 PID，否则返回 {@code null}。
     */
    public Integer pid() {
        return pid;
    }
}
