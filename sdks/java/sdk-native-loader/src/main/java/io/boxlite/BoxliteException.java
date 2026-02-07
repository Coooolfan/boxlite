package io.boxlite;

/** Java SDK 错误的基础异常类型。 */
public class BoxliteException extends RuntimeException {
    /**
     * 使用错误信息创建异常。
     *
     * @param message 错误信息。
     */
    public BoxliteException(String message) {
        super(message);
    }

    /**
     * 使用错误信息和根因创建异常。
     *
     * @param message 错误信息。
     * @param cause 根因异常。
     */
    public BoxliteException(String message, Throwable cause) {
        super(message, cause);
    }
}
