package io.boxlite;

/** Error raised when a requested box or runtime resource cannot be found. */
public final class NotFoundException extends BoxliteException {
    public NotFoundException(String message) {
        super(message);
    }
}
