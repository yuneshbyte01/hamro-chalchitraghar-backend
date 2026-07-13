package com.chalchitraghar.shared.exception;

/** Exception thrown when an invalid seat selection occurs. */
public class InvalidSeatSelectionException extends RuntimeException {
    /**
     * Constructs a new InvalidSeatSelectionException with the specified message.
     *
     * @param message the message to be associated with the exception
     */
    public InvalidSeatSelectionException(String message) {
        super(message);
    }
}
