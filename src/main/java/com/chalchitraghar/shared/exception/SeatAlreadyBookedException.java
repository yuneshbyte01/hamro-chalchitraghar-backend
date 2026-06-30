package com.chalchitraghar.shared.exception;

/**
 * Exception thrown when a seat is already booked.
 */
public class SeatAlreadyBookedException extends RuntimeException {
    
    /**
     * Constructs a new SeatAlreadyBookedException with the specified message.
     * @param message the message to be associated with the exception
     */
    public SeatAlreadyBookedException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new SeatAlreadyBookedException with the specified seat ID.
     * @param seatId the ID of the seat that is already booked
     */
    public SeatAlreadyBookedException(Long seatId) {
        super(String.format("Seat with id %d is already booked", seatId));
    }
}
