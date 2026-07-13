package com.chalchitraghar.modules.seats.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically releases expired seat holds. */
@Component
@RequiredArgsConstructor
public class ExpiredSeatLockCleanupJob {

    private final SeatLockService seatLockService;

    @Scheduled(fixedDelay = 60000)
    public void releaseExpiredSeatLocks() {
        seatLockService.releaseExpiredSeatLocks();
    }
}
