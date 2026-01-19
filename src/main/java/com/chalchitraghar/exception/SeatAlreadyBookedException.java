package com.chalchitraghar.exception;

public class SeatAlreadyBookedException extends RuntimeException {
    
    public SeatAlreadyBookedException(String message) {
        super(message);
    }
    
    public SeatAlreadyBookedException(Long seatId) {
        super(String.format("Seat with id %d is already booked", seatId));
    }
}
