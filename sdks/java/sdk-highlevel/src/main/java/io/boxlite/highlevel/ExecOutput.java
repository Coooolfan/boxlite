package io.boxlite.highlevel;

/** 高层命令执行的聚合结果。 */
public final class ExecOutput {
    private final int exitCode;
    private final String stdout;
    private final String stderr;
    private final String errorMessage;

    ExecOutput(int exitCode, String stdout, String stderr, String errorMessage) {
        this.exitCode = exitCode;
        this.stdout = stdout;
        this.stderr = stderr;
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
     * 返回捕获到的标准输出。
     *
     * @return 标准输出文本。
     */
    public String stdout() {
        return stdout;
    }

    /**
     * 返回捕获到的标准错误。
     *
     * @return 标准错误文本。
     */
    public String stderr() {
        return stderr;
    }

    /**
     * 返回可选的运行时/原生错误信息。
     *
     * @return 错误信息，或 {@code null}。
     */
    public String errorMessage() {
        return errorMessage;
    }

    /**
     * 返回命令是否执行成功。
     *
     * @return 退出码为 0 时返回 {@code true}。
     */
    public boolean success() {
        return exitCode == 0;
    }
}
