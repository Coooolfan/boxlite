package io.boxlite;

/** Base exception type for Java SDK errors. */
public class BoxliteException extends RuntimeException {
    /**
     * Creates a BoxLite exception with message.
     *
     * @param message error message
     */
    public BoxliteException(String message) {
        super(message);
    }

    /**
     * Creates a BoxLite exception with message and cause.
     *
     * @param message error message
     * @param cause root cause
     */
    public BoxliteException(String message, Throwable cause) {
        super(message, cause);
    }
}
