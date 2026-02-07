package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 运行时全局指标快照。 */
public final class RuntimeMetrics {
    private final long boxesCreatedTotal;
    private final long boxesFailedTotal;
    private final long boxesStoppedTotal;
    private final long numRunningBoxes;
    private final long totalCommandsExecuted;
    private final long totalExecErrors;

    /**
     * 可反序列化的运行时指标模型。
     *
     * @param boxesCreatedTotal 累计创建盒子数。
     * @param boxesFailedTotal 累计失败盒子操作数。
     * @param boxesStoppedTotal 累计停止盒子数。
     * @param numRunningBoxes 当前运行中盒子数。
     * @param totalCommandsExecuted 累计执行命令数。
     * @param totalExecErrors 累计命令执行错误数。
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
     * 返回运行时启动以来累计创建盒子数。
     *
     * @return 创建计数器。
     */
    public long boxesCreatedTotal() {
        return boxesCreatedTotal;
    }

    /**
     * 返回累计失败盒子操作数。
     *
     * @return 失败计数器。
     */
    public long boxesFailedTotal() {
        return boxesFailedTotal;
    }

    /**
     * 返回累计停止盒子数。
     *
     * @return 停止计数器。
     */
    public long boxesStoppedTotal() {
        return boxesStoppedTotal;
    }

    /**
     * 返回当前运行中盒子数。
     *
     * @return 运行中盒子数量。
     */
    public long numRunningBoxes() {
        return numRunningBoxes;
    }

    /**
     * 返回累计执行命令数。
     *
     * @return 命令计数器。
     */
    public long totalCommandsExecuted() {
        return totalCommandsExecuted;
    }

    /**
     * 返回累计执行错误数。
     *
     * @return 执行错误计数器。
     */
    public long totalExecErrors() {
        return totalExecErrors;
    }
}
