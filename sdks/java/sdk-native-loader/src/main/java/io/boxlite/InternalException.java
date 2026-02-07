package io.boxlite;

/** 内部或原生侧失败时抛出的异常。 */
public final class InternalException extends BoxliteException {
    /**
     * 使用内部错误信息创建异常。
     *
     * @param message 错误信息。
     */
    public InternalException(String message) {
        super(message);
    }

    /**
     * 使用内部错误信息和根因创建异常。
     *
     * @param message 错误信息。
     * @param cause 根因异常。
     */
    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
