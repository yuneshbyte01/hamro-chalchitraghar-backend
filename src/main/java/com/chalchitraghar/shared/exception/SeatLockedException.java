package com.chalchitraghar.shared.exception;

/** Exception thrown when a seat is locked. */
public class SeatLockedException extends RuntimeException {
    /**
     * Constructs a new SeatLockedException with the specified message.
     *
     * @param message the message to be associated with the exception
     */
    public SeatLockedException(String message) {
        super(message);
    }

    /**
     * Constructs a new SeatLockedException with the specified seat ID.
     *
     * @param seatId the ID of the seat that is currently locked
     */
    public SeatLockedException(Long seatId) {
        super(String.format("Seat with id %d is currently locked", seatId));
    }
}
