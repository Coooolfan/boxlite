package io.boxlite;

/** Error raised for invalid SDK configuration or argument values. */
public final class ConfigException extends BoxliteException {
    /**
     * Creates a configuration exception.
     *
     * @param message error message
     */
    public ConfigException(String message) {
        super(message);
    }
}
