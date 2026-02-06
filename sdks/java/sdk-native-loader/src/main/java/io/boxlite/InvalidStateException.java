package io.boxlite;

/** Error raised when a runtime or box handle is closed or in an invalid state. */
public final class InvalidStateException extends BoxliteException {
    public InvalidStateException(String message) {
        super(message);
    }
}
