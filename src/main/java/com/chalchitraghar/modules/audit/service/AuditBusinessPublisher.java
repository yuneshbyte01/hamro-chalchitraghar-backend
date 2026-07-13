package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.factory.*;
import com.chalchitraghar.modules.users.entity.User;
import java.util.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Explicit publication boundary for synchronous business services. */
@Component
@RequiredArgsConstructor
public class AuditBusinessPublisher {
    private final AuditEventFactory factory;
    private final AuditActorResolver actors;
    private final AuditEventPublisher publisher;

    public void catalog(
            AuditAction action,
            String type,
            Long id,
            String reference,
            Map<String, ?> before,
            Map<String, ?> after,
            AuditSeverity severity) {
        publish(
                action + ":" + id + ":" + UUID.randomUUID(),
                actors.currentUserOrSystem(),
                action,
                AuditCategory.CATALOG,
                severity,
                type,
                id,
                reference,
                before,
                after,
                null);
    }

    public void userChange(
            AuditAction action,
            User actor,
            User target,
            Map<String, ?> before,
            Map<String, ?> after,
            AuditSeverity severity) {
        publish(
                action + ":" + target.getId() + ":" + UUID.randomUUID(),
                actors.user(actor),
                action,
                AuditCategory.USER_MANAGEMENT,
                severity,
                "USER",
                target.getId(),
                String.valueOf(target.getId()),
                before,
                after,
                null);
    }

    public void notificationPreference(User actor, String type, boolean before, boolean after) {
        if (before == after) return;
        publish(
                "NOTIFICATION_PREFERENCE_UPDATED:"
                        + actor.getId()
                        + ":"
                        + type
                        + ":"
                        + UUID.randomUUID(),
                actors.user(actor),
                AuditAction.NOTIFICATION_PREFERENCE_UPDATED,
                AuditCategory.NOTIFICATION,
                AuditSeverity.INFO,
                "NOTIFICATION_PREFERENCE",
                actor.getId(),
                type,
                Map.of("type", type, "channel", "EMAIL", "enabled", before),
                Map.of("type", type, "channel", "EMAIL", "enabled", after),
                null);
    }

    public void notificationRetry(Long deliveryId, String before, String after, int attempts) {
        publish(
                "NOTIFICATION_DELIVERY_RETRIED:" + deliveryId + ":" + UUID.randomUUID(),
                actors.currentUserOrSystem(),
                AuditAction.NOTIFICATION_DELIVERY_RETRIED,
                AuditCategory.NOTIFICATION,
                AuditSeverity.WARNING,
                "NOTIFICATION_DELIVERY",
                deliveryId,
                String.valueOf(deliveryId),
                Map.of("status", before),
                Map.of("status", after),
                Map.of("attemptNumber", attempts));
    }

    public void show(AuditAction action, Long id, Map<String, ?> before, Map<String, ?> after) {
        publish(
                action + ":" + id + ":" + UUID.randomUUID(),
                actors.currentUserOrSystem(),
                action,
                AuditCategory.SHOW_MANAGEMENT,
                AuditSeverity.INFO,
                "SHOW",
                id,
                String.valueOf(id),
                before,
                after,
                null);
    }

    public void systemShowTransition(Long id, String before, String after) {
        publish(
                "SHOW_STATUS_RECONCILED:" + id + ":" + before + ":" + after,
                actors.system(),
                AuditAction.SHOW_STATUS_RECONCILED,
                AuditCategory.SHOW_MANAGEMENT,
                AuditSeverity.INFO,
                "SHOW",
                id,
                String.valueOf(id),
                Map.of("status", before),
                Map.of("status", after),
                null);
    }

    public void passwordReset(User user) {
        publish(
                "PASSWORD_RESET_COMPLETED:" + user.getId() + ":" + UUID.randomUUID(),
                actors.user(user),
                AuditAction.PASSWORD_RESET_COMPLETED,
                AuditCategory.AUTHENTICATION,
                AuditSeverity.HIGH,
                "USER",
                user.getId(),
                String.valueOf(user.getId()),
                null,
                Map.of("id", user.getId()),
                null);
    }

    public void ticket(
            AuditAction action,
            User actor,
            Long ticketId,
            String reference,
            Map<String, ?> before,
            Map<String, ?> after,
            Map<String, ?> metadata) {
        publish(
                action
                        + ":"
                        + (metadata != null && metadata.containsKey("validationId")
                                ? metadata.get("validationId")
                                : ticketId + ":" + UUID.randomUUID()),
                actor == null ? actors.system() : actors.user(actor),
                action,
                AuditCategory.TICKET,
                AuditSeverity.INFO,
                "TICKET",
                ticketId,
                reference,
                before,
                after,
                metadata);
    }

    public void paymentInitiated(
            User actor,
            Long id,
            String reference,
            String bookingReference,
            String provider,
            String method,
            Object amount,
            String currency) {
        publish(
                "PAYMENT_INITIATED:" + id,
                actors.user(actor),
                AuditAction.PAYMENT_INITIATED,
                AuditCategory.PAYMENT,
                AuditSeverity.INFO,
                "PAYMENT",
                id,
                reference,
                null,
                Map.of(
                        "paymentReference",
                        reference,
                        "bookingReference",
                        bookingReference,
                        "provider",
                        provider,
                        "method",
                        method,
                        "amount",
                        amount,
                        "currency",
                        currency,
                        "status",
                        "CREATED"),
                null);
    }

    private void publish(
            String eventId,
            com.chalchitraghar.modules.audit.event.AuditActor actor,
            AuditAction action,
            AuditCategory category,
            AuditSeverity severity,
            String type,
            Long id,
            String reference,
            Map<String, ?> before,
            Map<String, ?> after,
            Map<String, ?> metadata) {
        publisher.publish(
                factory.success(
                        eventId, actor, action, category, severity, type, id, reference, before,
                        after, metadata));
    }
}
