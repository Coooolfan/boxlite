package io.boxlite;

/** Error raised when a requested box or runtime resource cannot be found. */
public final class NotFoundException extends BoxliteException {
    /**
     * Creates a not-found exception.
     *
     * @param message error message
     */
    public NotFoundException(String message) {
        super(message);
    }
}
