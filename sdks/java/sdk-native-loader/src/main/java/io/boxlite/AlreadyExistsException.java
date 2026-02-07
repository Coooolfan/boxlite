package io.boxlite;

/** 当尝试创建已存在资源时抛出的异常。 */
public final class AlreadyExistsException extends BoxliteException {
    /**
     * 使用已存在错误信息创建异常。
     *
     * @param message 错误信息。
     */
    public AlreadyExistsException(String message) {
        super(message);
    }
}
