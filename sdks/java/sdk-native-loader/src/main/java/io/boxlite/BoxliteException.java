package io.boxlite;

/** Base exception type for Java SDK errors. */
public class BoxliteException extends RuntimeException {
    public BoxliteException(String message) {
        super(message);
    }

    public BoxliteException(String message, Throwable cause) {
        super(message, cause);
    }
}
