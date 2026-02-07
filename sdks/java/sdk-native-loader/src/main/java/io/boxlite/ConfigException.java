package io.boxlite;

/** Error raised for invalid SDK configuration or argument values. */
public final class ConfigException extends BoxliteException {
    public ConfigException(String message) {
        super(message);
    }
}
