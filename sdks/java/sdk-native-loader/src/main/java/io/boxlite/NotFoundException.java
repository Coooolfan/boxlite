package io.boxlite;

/** 当请求的盒子或运行时资源不存在时抛出的异常。 */
public final class NotFoundException extends BoxliteException {
    /**
     * 使用未找到错误信息创建异常。
     *
     * @param message 错误信息。
     */
    public NotFoundException(String message) {
        super(message);
    }
}
