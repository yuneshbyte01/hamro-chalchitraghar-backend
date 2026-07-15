package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.audit.enums.AuditAction;
import com.chalchitraghar.modules.notifications.event.RefundOperationalAuditEvent;
import com.chalchitraghar.modules.payments.config.RefundProcessingProperties;
import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.enums.RefundAttemptStatus;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import com.chalchitraghar.modules.payments.repository.RefundAttemptRepository;
import com.chalchitraghar.modules.payments.repository.RefundRepository;
import com.chalchitraghar.shared.exception.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaleRefundRecoveryService {
    private final RefundRepository refunds;
    private final RefundProcessingProperties properties;
    private final Clock clock;
    private final RefundAttemptRepository attempts;
    private final ApplicationEventPublisher events;

    @Transactional
    public int recover() {
        LocalDateTime now = LocalDateTime.now(clock);
        int count = 0;
        for (Refund candidate :
                refunds.findTop50ByStatusAndClaimedAtBeforeOrderByClaimedAtAsc(
                        RefundStatus.PROCESSING, now.minus(properties.getProcessingTimeout()))) {
            Refund r =
                    refunds.findByRefundReferenceForUpdate(candidate.getRefundReference())
                            .orElseThrow();
            if (r.getStatus() != RefundStatus.PROCESSING
                    || r.getClaimedAt() == null
                    || !r.getClaimedAt().isBefore(now.minus(properties.getProcessingTimeout())))
                continue;
            r.setStatus(RefundStatus.MANUAL_REVIEW);
            r.setManualReviewRequired(true);
            r.setLastFailureCode("PROCESSING_TIMEOUT");
            r.setFailureReason("Processing outcome requires review");
            r.setClaimedAt(null);
            r.setClaimedBy(null);
            r.setNextAttemptAt(null);
            r.setUpdatedAt(now);
            attempts.findForUpdate(r.getId(), r.getAttemptCount())
                    .ifPresent(
                            a -> {
                                a.setStatus(RefundAttemptStatus.UNKNOWN);
                                a.setCompletedAt(now);
                                a.setFailureCode("PROCESSING_TIMEOUT");
                                a.setFailureReason("Processing outcome requires review");
                                a.setUpdatedAt(now);
                            });
            count++;
        }
        return count;
    }

    @Transactional
    public Refund recover(String reference) {
        LocalDateTime now = LocalDateTime.now(clock);
        Refund r =
                refunds.findByRefundReferenceForUpdate(reference.trim().toUpperCase())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Refund not found with reference: " + reference));
        if (r.getStatus() != RefundStatus.PROCESSING
                || r.getClaimedAt() == null
                || !r.getClaimedAt().isBefore(now.minus(properties.getProcessingTimeout())))
            throw new PaymentConflictException("Refund is not stale processing");
        r.setStatus(RefundStatus.MANUAL_REVIEW);
        r.setManualReviewRequired(true);
        r.setLastFailureCode("PROCESSING_TIMEOUT");
        r.setFailureReason("Processing outcome requires review");
        r.setClaimedAt(null);
        r.setClaimedBy(null);
        r.setNextAttemptAt(null);
        r.setUpdatedAt(now);
        attempts.findForUpdate(r.getId(), r.getAttemptCount())
                .ifPresent(
                        a -> {
                            a.setStatus(RefundAttemptStatus.UNKNOWN);
                            a.setCompletedAt(now);
                            a.setFailureCode("PROCESSING_TIMEOUT");
                            a.setFailureReason("Processing outcome requires review");
                            a.setUpdatedAt(now);
                        });
        Refund saved = refunds.save(r);
        events.publishEvent(
                new RefundOperationalAuditEvent(
                        r.getId(),
                        r.getRefundReference(),
                        null,
                        AuditAction.REFUND_STALE_PROCESSING_RECOVERED,
                        "PROCESSING",
                        "MANUAL_REVIEW",
                        now));
        return saved;
    }
}
