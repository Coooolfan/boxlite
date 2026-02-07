package io.boxlite;

/** 当 SDK 配置或参数非法时抛出的异常。 */
public final class ConfigException extends BoxliteException {
    /**
     * 使用配置错误信息创建异常。
     *
     * @param message 错误信息。
     */
    public ConfigException(String message) {
        super(message);
    }
}
