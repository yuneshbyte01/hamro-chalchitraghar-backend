package com.chalchitraghar.modules.seats.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Periodically releases expired seat holds. */
@Component
@RequiredArgsConstructor
public class ExpiredSeatLockCleanupJob {

    private final SeatLockService seatLockService;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelay = 60000)
    public void releaseExpiredSeatLocks() {
        jobs.observe(
                JobName.SEAT_LOCK_EXPIRY,
                () -> {
                    seatLockService.releaseExpiredSeatLocks();
                    return 0;
                });
    }
}
