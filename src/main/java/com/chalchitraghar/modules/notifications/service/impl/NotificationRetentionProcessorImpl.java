package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.config.NotificationRetentionProperties;
import com.chalchitraghar.modules.notifications.entity.*;
import com.chalchitraghar.modules.notifications.enums.NotificationDeliveryStatus;
import com.chalchitraghar.modules.notifications.repository.*;
import com.chalchitraghar.modules.notifications.service.NotificationRetentionProcessor;
import java.time.*;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationRetentionProcessorImpl implements NotificationRetentionProcessor {
    private static final Set<NotificationDeliveryStatus> TERMINAL =
            Set.of(
                    NotificationDeliveryStatus.SENT,
                    NotificationDeliveryStatus.EXHAUSTED,
                    NotificationDeliveryStatus.SKIPPED);
    private final NotificationRepository notifications;
    private final NotificationDeliveryRepository deliveries;
    private final NotificationRetentionProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public int processBatch() {
        if (!properties.isEnabled()) return 0;
        LocalDateTime now = LocalDateTime.now(clock);
        int size = properties.getBatchSize();
        Specification<NotificationDelivery> deliverySpec =
                (root, query, cb) ->
                        cb.and(
                                root.get("status").in(TERMINAL),
                                cb.lessThan(
                                        root.get("createdAt"),
                                        now.minusDays(properties.getDeliveryDays())),
                                cb.isNull(root.get("anonymizedAt")));
        var oldDeliveries =
                deliveries.findAll(deliverySpec, PageRequest.of(0, size, Sort.by("createdAt")));
        oldDeliveries.forEach(
                d -> {
                    d.setRecipient(null);
                    d.setFailureReason(
                            d.getFailureReason() == null ? null : "Archived delivery result");
                    d.setClaimedBy(null);
                    d.setAnonymizedAt(now);
                    d.setUpdatedAt(now);
                });
        int remaining = size - oldDeliveries.getNumberOfElements();
        if (remaining <= 0) return oldDeliveries.getNumberOfElements();
        Specification<Notification> notificationSpec =
                (root, query, cb) -> {
                    var sub = query.subquery(Long.class);
                    var d = sub.from(NotificationDelivery.class);
                    sub.select(d.get("id"))
                            .where(
                                    cb.equal(d.get("notification").get("id"), root.get("id")),
                                    d.get("status")
                                            .in(
                                                    Set.of(
                                                            NotificationDeliveryStatus.PENDING,
                                                            NotificationDeliveryStatus.PROCESSING,
                                                            NotificationDeliveryStatus.FAILED)));
                    return cb.and(
                            cb.isTrue(root.get("read")),
                            cb.lessThan(
                                    root.get("createdAt"),
                                    now.minusDays(properties.getNotificationDays())),
                            cb.isNull(root.get("anonymizedAt")),
                            cb.not(cb.exists(sub)));
                };
        var oldNotifications =
                notifications.findAll(
                        notificationSpec, PageRequest.of(0, remaining, Sort.by("createdAt")));
        oldNotifications.forEach(
                n -> {
                    n.setTitle("Archived notification");
                    n.setMessage("Notification content removed by retention policy.");
                    n.setPayload(null);
                    n.setAnonymizedAt(now);
                    n.setUpdatedAt(now);
                });
        return oldDeliveries.getNumberOfElements() + oldNotifications.getNumberOfElements();
    }
}
