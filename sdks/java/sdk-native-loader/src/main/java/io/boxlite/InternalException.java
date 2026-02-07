package io.boxlite;

/** Error raised for internal/native-side failures. */
public final class InternalException extends BoxliteException {
    /**
     * Creates an internal exception.
     *
     * @param message error message
     */
    public InternalException(String message) {
        super(message);
    }

    /**
     * Creates an internal exception with cause.
     *
     * @param message error message
     * @param cause root cause
     */
    public InternalException(String message, Throwable cause) {
        super(message, cause);
    }
}
