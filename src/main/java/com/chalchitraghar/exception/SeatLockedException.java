package com.chalchitraghar.exception;

public class SeatLockedException extends RuntimeException {
    
    public SeatLockedException(String message) {
        super(message);
    }
    
    public SeatLockedException(Long seatId) {
        super(String.format("Seat with id %d is currently locked", seatId));
    }
}
