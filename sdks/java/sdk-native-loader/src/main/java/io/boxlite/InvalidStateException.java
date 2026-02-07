package io.boxlite;

/** 当运行时或盒子句柄已关闭或状态非法时抛出的异常。 */
public final class InvalidStateException extends BoxliteException {
    /**
     * 使用非法状态错误信息创建异常。
     *
     * @param message 错误信息。
     */
    public InvalidStateException(String message) {
        super(message);
    }
}
