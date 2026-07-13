package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.config.NotificationEmailProperties;
import com.chalchitraghar.modules.notifications.enums.NotificationDeliveryStatus;
import com.chalchitraghar.modules.notifications.repository.NotificationDeliveryRepository;
import com.chalchitraghar.modules.notifications.service.StaleNotificationDeliveryRecoveryService;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaleNotificationDeliveryRecoveryServiceImpl
        implements StaleNotificationDeliveryRecoveryService {
    private final NotificationDeliveryRepository deliveries;
    private final NotificationEmailProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public int recoverBatch() {
        LocalDateTime now = LocalDateTime.now(clock);
        var stale =
                deliveries.findByStatusAndClaimedAtLessThanEqualOrderByClaimedAtAsc(
                        NotificationDeliveryStatus.PROCESSING,
                        now.minus(Duration.ofMillis(properties.getProcessingTimeoutMs())),
                        PageRequest.of(0, properties.getRetryBatchSize()));
        int recovered = 0;
        for (var candidate : stale) {
            var delivery = deliveries.findByIdForUpdate(candidate.getId()).orElse(null);
            if (delivery == null
                    || delivery.getStatus() != NotificationDeliveryStatus.PROCESSING
                    || delivery.getClaimedAt() == null
                    || delivery.getClaimedAt()
                            .plus(Duration.ofMillis(properties.getProcessingTimeoutMs()))
                            .isAfter(now)) continue;
            delivery.setStatus(
                    delivery.getAttemptCount() >= delivery.getMaxAttempts()
                            ? NotificationDeliveryStatus.EXHAUSTED
                            : NotificationDeliveryStatus.FAILED);
            delivery.setFailureReason("Recovered stale processing claim");
            delivery.setFailedAt(now);
            delivery.setNextAttemptAt(
                    delivery.getStatus() == NotificationDeliveryStatus.FAILED ? now : null);
            delivery.setClaimedAt(null);
            delivery.setClaimedBy(null);
            delivery.setUpdatedAt(now);
            recovered++;
        }
        return recovered;
    }
}
