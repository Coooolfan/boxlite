package io.boxlite;

/** Error raised when attempting to create a resource that already exists. */
public final class AlreadyExistsException extends BoxliteException {
    public AlreadyExistsException(String message) {
        super(message);
    }
}
