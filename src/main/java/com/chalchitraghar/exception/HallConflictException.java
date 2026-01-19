package com.chalchitraghar.exception;

/**
 * Exception thrown when a hall conflict occurs.
 */
public class HallConflictException extends RuntimeException {
    /**
     * Constructs a new HallConflictException with the specified message.
     * @param message the message to be associated with the exception
     */
    public HallConflictException(String message) {
        super(message);
    }
}
