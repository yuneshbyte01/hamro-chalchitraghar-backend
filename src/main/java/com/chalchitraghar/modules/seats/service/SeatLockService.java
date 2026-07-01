package com.chalchitraghar.modules.seats.service;

import java.util.List;

import com.chalchitraghar.modules.bookings.dto.response.SeatHoldResponse;

/**
 * Service for seat hold and lock management.
 */
public interface SeatLockService {

    SeatHoldResponse holdSeats(Long showId, List<Long> seatIds, Long userId);

    void releaseSeatLocks(List<Long> seatIds);

    int releaseExpiredSeatLocks();
}
