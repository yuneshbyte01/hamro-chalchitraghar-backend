package com.chalchitraghar.modules.notifications.service.impl;

import com.chalchitraghar.modules.notifications.dto.response.*;
import com.chalchitraghar.modules.notifications.entity.*;
import com.chalchitraghar.modules.notifications.enums.*;
import com.chalchitraghar.modules.notifications.repository.*;
import com.chalchitraghar.modules.notifications.service.*;
import com.chalchitraghar.shared.exception.*;
import com.chalchitraghar.shared.response.PageResponse;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

@Service
@RequiredArgsConstructor
public class AdminNotificationOperationsServiceImpl implements AdminNotificationOperationsService {
    private final NotificationRepository notifications;
    private final NotificationDeliveryRepository deliveries;
    private final NotificationEmailDispatchLauncher launcher;
    private final ObjectMapper json;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminNotificationSummaryResponse> notifications(
            int page,
            int size,
            Long userId,
            String email,
            NotificationType type,
            NotificationChannel channel,
            Boolean read,
            LocalDateTime from,
            LocalDateTime to,
            NotificationDeliveryStatus deliveryStatus,
            String eventKey) {
        validatePage(page, size);
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("Invalid date range");
        Specification<Notification> spec =
                (root, query, cb) -> {
                    var predicates =
                            new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    if (userId != null)
                        predicates.add(cb.equal(root.get("user").get("id"), userId));
                    if (email != null && !email.isBlank())
                        predicates.add(
                                cb.like(
                                        cb.lower(root.get("user").get("email")),
                                        "%" + email.trim().toLowerCase() + "%"));
                    if (type != null) predicates.add(cb.equal(root.get("type"), type));
                    if (channel != null) predicates.add(cb.equal(root.get("channel"), channel));
                    if (read != null) predicates.add(cb.equal(root.get("read"), read));
                    if (from != null)
                        predicates.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), from));
                    if (to != null)
                        predicates.add(cb.lessThanOrEqualTo(root.get("occurredAt"), to));
                    if (eventKey != null && !eventKey.isBlank())
                        predicates.add(cb.equal(root.get("eventKey"), eventKey.trim()));
                    return cb.and(
                            predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };
        // Delivery status uses an existence subquery because Notification intentionally has no
        // back-reference.
        if (deliveryStatus != null)
            spec =
                    spec.and(
                            (root, query, cb) -> {
                                var sub = query.subquery(Long.class);
                                var d = sub.from(NotificationDelivery.class);
                                sub.select(d.get("id"))
                                        .where(
                                                cb.equal(
                                                        d.get("notification").get("id"),
                                                        root.get("id")),
                                                cb.equal(d.get("status"), deliveryStatus));
                                return cb.exists(sub);
                            });
        var result =
                notifications.findAll(
                        spec,
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
        return PageResponse.from(result, result.stream().map(this::summary).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminNotificationDetailResponse notification(Long id) {
        Notification n =
                notifications
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
        var related =
                deliveries.findAll(
                        (root, query, cb) -> cb.equal(root.get("notification").get("id"), id),
                        Sort.by("id"));
        return new AdminNotificationDetailResponse(
                n.getId(),
                n.getUser().getId(),
                n.getUser().getName(),
                mask(n.getUser().getEmail()),
                n.getType(),
                n.getChannel(),
                n.getEventKey(),
                n.getTitle(),
                n.getMessage(),
                safePayload(n.getPayload()),
                n.isRead(),
                n.getReadAt(),
                n.getOccurredAt(),
                n.getCreatedAt(),
                n.getUpdatedAt(),
                related.stream().map(this::deliveryDetail).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminNotificationDeliverySummaryResponse> deliveries(
            int page,
            int size,
            NotificationDeliveryStatus status,
            NotificationType type,
            Integer min,
            Integer max) {
        validatePage(page, size);
        if (min != null && min < 0
                || max != null && max < 0
                || min != null && max != null && min > max)
            throw new IllegalArgumentException("Invalid attempt range");
        Specification<NotificationDelivery> spec =
                (root, query, cb) -> {
                    var p = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                    if (status != null) p.add(cb.equal(root.get("status"), status));
                    if (type != null) p.add(cb.equal(root.get("notification").get("type"), type));
                    if (min != null) p.add(cb.greaterThanOrEqualTo(root.get("attemptCount"), min));
                    if (max != null) p.add(cb.lessThanOrEqualTo(root.get("attemptCount"), max));
                    return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
                };
        var result =
                deliveries.findAll(
                        spec,
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.from(result, result.stream().map(this::deliverySummary).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminNotificationDeliveryDetailResponse delivery(Long id) {
        return deliveryDetail(
                deliveries
                        .findById(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Notification delivery", id)));
    }

    @Override
    @Transactional
    public ManualDeliveryRetryResponse retry(Long id) {
        NotificationDelivery d =
                deliveries
                        .findByIdForUpdate(id)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Notification delivery", id));
        if (d.getStatus() != NotificationDeliveryStatus.FAILED
                || d.getAttemptCount() >= d.getMaxAttempts())
            throw new PaymentConflictException("Delivery is not eligible for manual retry");
        LocalDateTime now = LocalDateTime.now(clock);
        d.setStatus(NotificationDeliveryStatus.PENDING);
        d.setNextAttemptAt(now);
        d.setClaimedAt(null);
        d.setClaimedBy(null);
        d.setUpdatedAt(now);
        deliveries.save(d);
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        launcher.submit(id);
                    }
                });
        return new ManualDeliveryRetryResponse(id, true, "QUEUED");
    }

    private AdminNotificationSummaryResponse summary(Notification n) {
        return new AdminNotificationSummaryResponse(
                n.getId(),
                n.getUser().getId(),
                n.getUser().getName(),
                mask(n.getUser().getEmail()),
                n.getType(),
                n.getChannel(),
                n.getTitle(),
                n.isRead(),
                n.getOccurredAt(),
                n.getCreatedAt());
    }

    private AdminNotificationDeliverySummaryResponse deliverySummary(NotificationDelivery d) {
        return new AdminNotificationDeliverySummaryResponse(
                d.getId(),
                d.getChannel(),
                mask(d.getRecipient()),
                d.getStatus(),
                d.getAttemptCount(),
                d.getMaxAttempts(),
                d.getNextAttemptAt(),
                d.getLastAttemptAt(),
                d.getSentAt(),
                d.getFailedAt());
    }

    private AdminNotificationDeliveryDetailResponse deliveryDetail(NotificationDelivery d) {
        return new AdminNotificationDeliveryDetailResponse(
                d.getId(),
                d.getChannel(),
                mask(d.getRecipient()),
                d.getStatus(),
                d.getAttemptCount(),
                d.getMaxAttempts(),
                d.getNextAttemptAt(),
                d.getLastAttemptAt(),
                d.getSentAt(),
                d.getFailedAt(),
                d.getFailureReason(),
                d.getClaimedAt(),
                d.getClaimedBy(),
                d.getTemplateName(),
                d.getSubject(),
                d.getContentVersion(),
                d.getCreatedAt(),
                d.getUpdatedAt());
    }

    private JsonNode safePayload(String payload) {
        if (payload == null) return null;
        try {
            return json.readTree(payload);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String mask(String email) {
        if (email == null || !email.contains("@")) return "***";
        int at = email.indexOf('@');
        return email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be 1 to 100");
    }
}
