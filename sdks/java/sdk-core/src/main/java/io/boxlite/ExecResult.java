package io.boxlite;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/** 通过 {@link BoxHandle#exec(ExecCommand)} 创建的执行结果。 */
public final class ExecResult {
    private final int exitCode;
    private final String errorMessage;

    /**
     * 可反序列化的执行结果模型。
     *
     * @param exitCode 进程退出码。
     * @param errorMessage 来自原生层的可选错误信息。
     */
    @JsonCreator
    public ExecResult(
        @JsonProperty("exitCode") int exitCode,
        @JsonProperty("errorMessage") String errorMessage
    ) {
        this.exitCode = exitCode;
        this.errorMessage = errorMessage;
    }

    /**
     * 返回进程退出码。
     *
     * @return 退出码。
     */
    public int exitCode() {
        return exitCode;
    }

    /**
     * 返回可选错误信息。
     *
     * @return 原生侧错误信息，或 {@code null}。
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * 返回执行是否成功。
     *
     * @return 当 {@link #exitCode()} 为 0 时返回 {@code true}。
     */
    public boolean success() {
        return exitCode == 0;
    }
}
