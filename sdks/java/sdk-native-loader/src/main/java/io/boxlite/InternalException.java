package io.boxlite;

/** Error raised for internal/native-side failures. */
public final class InternalException extends BoxliteException {
    public InternalException(String message) {
        super(message);
    }

    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
