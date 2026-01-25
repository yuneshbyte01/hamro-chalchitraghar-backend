package com.chalchitraghar.service.booking;

import java.util.List;

/**
 * Service for seat booking validation and lock management.
 */
public interface SeatLockService {

    void validateAndLockSeats(Long showId, List<Long> seatIds, Long userId);

    void releaseSeatLocks(List<Long> seatIds);
}
