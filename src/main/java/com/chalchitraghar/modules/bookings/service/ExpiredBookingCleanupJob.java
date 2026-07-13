package com.chalchitraghar.modules.bookings.service;

import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpiredBookingCleanupJob {
    private final BookingRepository bookingRepository;
    private final BookingLifecycleService bookingLifecycleService;
    private final Clock clock;

    @Value("${app.bookings.expiry-batch-size:100}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${app.bookings.expiry-cleanup-interval-ms:60000}")
    public int expireBatch() {
        var batch =
                bookingRepository.findExpiredInitiatedBookings(
                        LocalDateTime.now(clock), PageRequest.of(0, batchSize));
        int expired = 0;
        for (var booking : batch.getContent()) {
            if (bookingLifecycleService.expireBooking(booking.getId(), true)) expired++;
        }
        return expired;
    }
}
