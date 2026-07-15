package com.chalchitraghar.modules.audit.listener;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.event.AuditEvent;
import com.chalchitraghar.modules.audit.factory.AuditActorResolver;
import com.chalchitraghar.modules.notifications.event.*;
import com.chalchitraghar.modules.tickets.service.TicketsIssuedEvent;
import java.time.Clock;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
@RequiredArgsConstructor
public class BusinessAuditEventBridge {
    private final AuditEventListener listener;
    private final AuditActorResolver actors;
    private final Clock clock;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(UserRegisteredEvent e) {
        success(
                "USER_REGISTERED:" + e.userId(),
                e.occurredAt(),
                actors.userId(e.userId()),
                AuditAction.USER_REGISTERED,
                AuditCategory.AUTHENTICATION,
                AuditSeverity.INFO,
                "USER",
                e.userId(),
                String.valueOf(e.userId()),
                null,
                Map.of("id", e.userId()),
                null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingCreatedEvent e) {
        booking(
                e.userId(),
                e.bookingId(),
                e.bookingReference(),
                e.occurredAt(),
                AuditAction.BOOKING_CREATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingConfirmedEvent e) {
        booking(
                e.userId(),
                e.bookingId(),
                e.bookingReference(),
                e.occurredAt(),
                AuditAction.BOOKING_CONFIRMED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingCancelledEvent e) {
        booking(
                e.userId(),
                e.bookingId(),
                e.bookingReference(),
                e.occurredAt(),
                AuditAction.BOOKING_CANCELLED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BookingExpiredEvent e) {
        success(
                "BOOKING_EXPIRED:" + e.bookingId(),
                e.occurredAt(),
                actors.system(),
                AuditAction.BOOKING_EXPIRED,
                AuditCategory.BOOKING,
                AuditSeverity.INFO,
                "BOOKING",
                e.bookingId(),
                e.bookingReference(),
                Map.of("status", "INITIATED"),
                Map.of("status", "EXPIRED"),
                null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentSucceededEvent e) {
        payment(e, AuditAction.PAYMENT_SUCCEEDED, AuditResult.SUCCESS);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PaymentFailedEvent e) {
        var event =
                new AuditEvent(
                        "PAYMENT_FAILED:" + e.paymentId(),
                        e.occurredAt(),
                        actors.external(),
                        AuditAction.PAYMENT_FAILED,
                        AuditCategory.PAYMENT,
                        AuditSeverity.WARNING,
                        AuditResult.FAILURE,
                        "PAYMENT",
                        e.paymentId(),
                        e.paymentReference(),
                        "Payment failed",
                        null,
                        Map.of("status", "FAILED"),
                        Map.of("provider", "ESEWA", "bookingReference", e.bookingReference()));
        listener.persist(event);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RefundRequestedEvent e) {
        refund(
                e.refundId(),
                e.refundReference(),
                e.actorUserId(),
                e.bookingReference(),
                e.paymentReference(),
                e.amount(),
                e.currency(),
                e.reason().name(),
                null,
                e.status().name(),
                e.occurredAt(),
                AuditAction.REFUND_CREATED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RefundApprovedEvent e) {
        refund(
                e.refundId(),
                e.refundReference(),
                e.actorUserId(),
                e.bookingReference(),
                e.paymentReference(),
                e.amount(),
                e.currency(),
                e.reason().name(),
                "REQUESTED",
                e.status().name(),
                e.occurredAt(),
                AuditAction.REFUND_APPROVED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(RefundRejectedEvent e) {
        refund(
                e.refundId(),
                e.refundReference(),
                e.actorUserId(),
                e.bookingReference(),
                e.paymentReference(),
                e.amount(),
                e.currency(),
                e.reason().name(),
                "REQUESTED",
                e.status().name(),
                e.occurredAt(),
                AuditAction.REFUND_REJECTED);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ShowUpdatedEvent e) {
        success(
                "SHOW_UPDATED:" + e.showId() + ":" + e.changeVersion(),
                e.occurredAt(),
                actors.currentUserOrSystem(),
                AuditAction.SHOW_UPDATED,
                AuditCategory.SHOW_MANAGEMENT,
                AuditSeverity.INFO,
                "SHOW",
                e.showId(),
                String.valueOf(e.showId()),
                Map.of("oldSchedule", e.oldSchedule().toString()),
                Map.of("newSchedule", e.newSchedule().toString()),
                null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(ShowCancelledEvent e) {
        success(
                "SHOW_CANCELLED:" + e.showId(),
                e.occurredAt(),
                actors.currentUserOrSystem(),
                AuditAction.SHOW_CANCELLED,
                AuditCategory.SHOW_MANAGEMENT,
                AuditSeverity.WARNING,
                "SHOW",
                e.showId(),
                String.valueOf(e.showId()),
                Map.of("status", "SCHEDULED"),
                Map.of("status", "CANCELLED"),
                null);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(TicketsIssuedEvent e) {
        success(
                "TICKET_ISSUED:" + e.bookingId(),
                java.time.LocalDateTime.now(clock),
                actors.system(),
                AuditAction.TICKET_ISSUED,
                AuditCategory.TICKET,
                AuditSeverity.INFO,
                "BOOKING",
                e.bookingId(),
                String.valueOf(e.bookingId()),
                null,
                null,
                Map.of("bookingId", e.bookingId()));
    }

    private void booking(
            Long userId,
            Long id,
            String reference,
            java.time.LocalDateTime at,
            AuditAction action) {
        success(
                action + ":" + id,
                at,
                actors.userId(userId),
                action,
                AuditCategory.BOOKING,
                AuditSeverity.INFO,
                "BOOKING",
                id,
                reference,
                null,
                Map.of("bookingReference", reference),
                null);
    }

    private void payment(PaymentSucceededEvent e, AuditAction action, AuditResult result) {
        listener.persist(
                new AuditEvent(
                        action + ":" + e.paymentId(),
                        e.occurredAt(),
                        actors.external(),
                        action,
                        AuditCategory.PAYMENT,
                        AuditSeverity.INFO,
                        result,
                        "PAYMENT",
                        e.paymentId(),
                        e.paymentReference(),
                        null,
                        null,
                        Map.of("status", "SUCCESS"),
                        Map.of("provider", "ESEWA", "bookingReference", e.bookingReference())));
    }

    private void refund(
            Long id,
            String reference,
            Long actorId,
            String bookingReference,
            String paymentReference,
            java.math.BigDecimal amount,
            String currency,
            String reason,
            String beforeStatus,
            String afterStatus,
            java.time.LocalDateTime at,
            AuditAction action) {
        success(
                action + ":" + id,
                at,
                actors.userId(actorId),
                action,
                AuditCategory.REFUND,
                AuditSeverity.INFO,
                "REFUND",
                id,
                reference,
                beforeStatus == null ? null : Map.of("status", beforeStatus),
                Map.of("status", afterStatus),
                Map.of(
                        "bookingReference", bookingReference,
                        "paymentReference", paymentReference,
                        "amount", amount.toPlainString(),
                        "currency", currency,
                        "reason", reason,
                        "type", "FULL",
                        "method", "MANUAL"));
    }

    private void success(
            String id,
            java.time.LocalDateTime at,
            com.chalchitraghar.modules.audit.event.AuditActor actor,
            AuditAction action,
            AuditCategory category,
            AuditSeverity severity,
            String type,
            Long resourceId,
            String reference,
            Map<String, ?> before,
            Map<String, ?> after,
            Map<String, ?> metadata) {
        listener.persist(
                new AuditEvent(
                        id,
                        at,
                        actor,
                        action,
                        category,
                        severity,
                        AuditResult.SUCCESS,
                        type,
                        resourceId,
                        reference,
                        null,
                        before,
                        after,
                        metadata));
    }
}
