package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.config.NotificationEmailProperties;
import com.chalchitraghar.modules.notifications.enums.NotificationDeliveryStatus;
import com.chalchitraghar.modules.notifications.repository.NotificationDeliveryRepository;
import com.chalchitraghar.modules.notifications.service.*;
import java.time.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationEmailRetryProcessorImpl implements NotificationEmailRetryProcessor {
    private final NotificationDeliveryRepository deliveries;
    private final NotificationEmailDispatchLauncher launcher;
    private final NotificationEmailProperties properties;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public int processBatch() {
        if (!properties.isEnabled()) return 0;
        LocalDateTime now = LocalDateTime.now(clock);
        var pageable = PageRequest.of(0, properties.getRetryBatchSize());
        var due =
                deliveries.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
                        List.of(
                                NotificationDeliveryStatus.PENDING,
                                NotificationDeliveryStatus.FAILED),
                        now,
                        pageable);
        int remaining = properties.getRetryBatchSize() - due.getNumberOfElements();
        var ids = new java.util.ArrayList<>(due.stream().map(d -> d.getId()).toList());
        if (remaining > 0) {
            LocalDateTime staleBefore =
                    now.minus(Duration.ofMillis(properties.getProcessingTimeoutMs()));
            ids.addAll(
                    deliveries
                            .findByStatusAndClaimedAtLessThanEqualOrderByClaimedAtAsc(
                                    NotificationDeliveryStatus.PROCESSING,
                                    staleBefore,
                                    PageRequest.of(0, remaining))
                            .stream()
                            .map(d -> d.getId())
                            .toList());
        }
        ids.forEach(launcher::submit);
        return ids.size();
    }
}
