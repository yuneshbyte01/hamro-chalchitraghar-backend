package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.audit.enums.AuditAction;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.notifications.event.RefundOperationalAuditEvent;
import com.chalchitraghar.modules.payments.config.RefundProcessingProperties;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.repository.*;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.tickets.repository.TicketRepository;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundConsistencyService {
    private final RefundRepository refunds;
    private final RefundAttemptRepository attempts;
    private final TicketRepository tickets;
    private final RefundProcessingProperties processing;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Transactional(readOnly = true)
    public AdminRefundConsistencyResponse check(String reference) {
        Refund r =
                refunds.findByRefundReference(reference.trim().toUpperCase())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Refund not found with reference: " + reference));
        List<AdminRefundConsistencyIssue> out = new ArrayList<>();
        if (r.getAmount().compareTo(r.getPayment().getAmount()) != 0)
            add(
                    out,
                    "REFUND_AMOUNT_MISMATCH",
                    "CRITICAL",
                    "Refund amount differs from its full payment",
                    "PAYMENT",
                    false);
        if (!r.getCurrency().equals(r.getPayment().getCurrency()))
            add(
                    out,
                    "CURRENCY_MISMATCH",
                    "CRITICAL",
                    "Refund and payment currencies differ",
                    "PAYMENT",
                    false);
        if (r.getStatus() == RefundStatus.SUCCEEDED
                && r.getPayment().getStatus() != PaymentStatus.REFUNDED)
            add(
                    out,
                    "PAYMENT_STATUS_MISMATCH",
                    "CRITICAL",
                    "Succeeded refund requires a refunded payment",
                    "PAYMENT",
                    true);
        var related = refunds.findByPaymentIdOrderByRequestedAtDescIdDesc(r.getPayment().getId());
        if (r.getPayment().getStatus() == PaymentStatus.REFUNDED
                && related.stream().noneMatch(x -> x.getStatus() == RefundStatus.SUCCEEDED))
            add(
                    out,
                    "PAYMENT_STATUS_MISMATCH",
                    "CRITICAL",
                    "Refunded payment has no succeeded refund",
                    "PAYMENT",
                    false);
        if (related.stream().filter(x -> x.getStatus() == RefundStatus.SUCCEEDED).count() > 1)
            add(
                    out,
                    "OVER_REFUNDED",
                    "CRITICAL",
                    "Payment has multiple succeeded full refunds",
                    "PAYMENT",
                    false);
        java.math.BigDecimal activeAmount =
                related.stream()
                        .filter(
                                x ->
                                        com.chalchitraghar.modules.payments.service
                                                .RefundBalanceService.RESERVING_STATUSES
                                                .contains(x.getStatus()))
                        .map(Refund::getAmount)
                        .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        if (activeAmount.compareTo(r.getPayment().getAmount()) > 0)
            add(
                    out,
                    "OVER_REFUNDED",
                    "CRITICAL",
                    "Active refund reservations exceed the payment amount",
                    "PAYMENT",
                    false);
        if (Set.of(RefundStatus.REQUESTED, RefundStatus.APPROVED).contains(r.getStatus())
                && r.getPayment().getStatus() != PaymentStatus.SUCCESS)
            add(
                    out,
                    "PAYMENT_STATUS_MISMATCH",
                    "ERROR",
                    "Pending refund requires a successful payment",
                    "PAYMENT",
                    false);
        if (r.getStatus() == RefundStatus.APPROVED
                && (r.getApprovedAt() == null || r.getApprovedBy() == null))
            add(
                    out,
                    "MISSING_DECISION_CONTEXT",
                    "ERROR",
                    "Approved refund lacks approval context",
                    "REFUND",
                    false);
        if (r.getStatus() == RefundStatus.REJECTED
                && (r.getRejectedAt() == null || r.getRejectedBy() == null))
            add(
                    out,
                    "MISSING_DECISION_CONTEXT",
                    "ERROR",
                    "Rejected refund lacks rejection context",
                    "REFUND",
                    false);
        if (r.getStatus() == RefundStatus.SUCCEEDED && r.getProcessedAt() == null)
            add(
                    out,
                    "MISSING_PROCESSED_TIMESTAMP",
                    "ERROR",
                    "Succeeded refund lacks processed timestamp",
                    "REFUND",
                    true);
        if (r.getAttemptCount() != attempts.findByRefundIdOrderByAttemptNumberAsc(r.getId()).size())
            add(
                    out,
                    "ATTEMPT_COUNT_MISMATCH",
                    "ERROR",
                    "Attempt count differs from persisted attempt history",
                    "REFUND_ATTEMPT",
                    false);
        if (r.getStatus() == RefundStatus.PROCESSING
                && r.getClaimedAt() != null
                && r.getClaimedAt()
                        .isBefore(
                                LocalDateTime.now(clock).minus(processing.getProcessingTimeout())))
            add(
                    out,
                    "STALE_PROCESSING",
                    "WARNING",
                    "Processing claim exceeded the configured timeout",
                    "REFUND",
                    true);
        if (Set.of(RefundReason.CUSTOMER_CANCELLATION, RefundReason.SHOW_CANCELLATION)
                        .contains(r.getReason())
                && r.getStatus() == RefundStatus.SUCCEEDED
                && r.getBooking().getStatus() != BookingStatus.CANCELLED)
            add(
                    out,
                    "BOOKING_STATUS_MISMATCH",
                    "CRITICAL",
                    "Cancellation refund requires a cancelled booking",
                    "BOOKING",
                    false);
        var ts = tickets.findByBookingIdOrderByIssuedAtAsc(r.getBooking().getId());
        if (r.getStatus() == RefundStatus.SUCCEEDED
                && ts.stream()
                        .anyMatch(
                                t ->
                                        t.getStatus() == TicketStatus.ISSUED
                                                || t.getStatus() == TicketStatus.CHECKED_IN))
            add(
                    out,
                    "TICKET_STATUS_MISMATCH",
                    "CRITICAL",
                    "Succeeded cancellation refund has an active ticket",
                    "TICKET",
                    false);
        events.publishEvent(
                new RefundOperationalAuditEvent(
                        r.getId(),
                        r.getRefundReference(),
                        null,
                        AuditAction.REFUND_CONSISTENCY_CHECKED,
                        r.getStatus().name(),
                        r.getStatus().name(),
                        LocalDateTime.now(clock)));
        return new AdminRefundConsistencyResponse(
                r.getRefundReference(),
                out.isEmpty(),
                out.size(),
                List.copyOf(out),
                LocalDateTime.now(clock));
    }

    private void add(
            List<AdminRefundConsistencyIssue> out,
            String c,
            String s,
            String d,
            String resource,
            boolean repairable) {
        out.add(new AdminRefundConsistencyIssue(c, s, d, resource, repairable));
    }
}
