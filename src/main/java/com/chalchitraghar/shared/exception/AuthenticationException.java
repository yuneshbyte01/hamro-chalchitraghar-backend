package com.chalchitraghar.shared.exception;

/** Exception thrown when authentication fails. */
public class AuthenticationException extends RuntimeException {

    /**
     * Constructs a new AuthenticationException with the specified message.
     *
     * @param message the message to be associated with the exception
     */
    public AuthenticationException(String message) {
        super(message);
    }
}
