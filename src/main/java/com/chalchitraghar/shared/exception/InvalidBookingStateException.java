package com.chalchitraghar.shared.exception;

/**
 * Exception thrown when a booking operation is attempted on a booking with an invalid state.
 */
public class InvalidBookingStateException extends RuntimeException {
    
    /**
     * Constructs a new InvalidBookingStateException with the specified message.
     * @param message the message to be associated with the exception
     */
    public InvalidBookingStateException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new InvalidBookingStateException with booking ID and current status.
     * @param bookingId the ID of the booking
     * @param currentStatus the current status of the booking
     * @param expectedStatus the expected status for the operation
     */
    public InvalidBookingStateException(Long bookingId, String currentStatus, String expectedStatus) {
        super(String.format("Booking %d is in %s state. Expected state: %s", bookingId, currentStatus, expectedStatus));
    }
}
