package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.audit.enums.AuditAction;
import com.chalchitraghar.modules.notifications.event.RefundOperationalAuditEvent;
import com.chalchitraghar.modules.payments.dto.request.*;
import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.repository.RefundRepository;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundAdminOperationsService {
    private final RefundRepository refunds;
    private final RefundOperationsService operations;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Transactional
    public Refund resolve(
            String reference, AdminResolveRefundManualReviewRequest request, User admin) {
        Refund r = lock(reference);
        if (r.getStatus() == RefundStatus.SUCCEEDED
                && request.resolution() == RefundManualReviewResolution.MARK_SUCCEEDED) {
            if (request.externalReference() == null || request.externalReference().isBlank())
                throw new IllegalArgumentException("externalReference is required for success");
            return operations.markManualSuccess(
                    reference,
                    new AdminManualRefundSuccessRequest(
                            request.externalReference(), request.note()),
                    admin);
        }
        if (r.getStatus() != RefundStatus.MANUAL_REVIEW)
            throw new PaymentConflictException("Only manual-review refunds can be resolved");
        String before = r.getStatus().name();
        Refund result =
                switch (request.resolution()) {
                    case MARK_SUCCEEDED -> {
                        if (request.externalReference() == null
                                || request.externalReference().isBlank())
                            throw new IllegalArgumentException(
                                    "externalReference is required for success");
                        yield operations.markManualSuccess(
                                reference,
                                new AdminManualRefundSuccessRequest(
                                        request.externalReference(), request.note()),
                                admin);
                    }
                    case MARK_FAILED -> terminal(r, "MANUAL_REVIEW_FAILED", request.note());
                    case RETRY -> retry(r);
                    case REJECT -> reject(r, admin);
                };
        events.publishEvent(
                new RefundOperationalAuditEvent(
                        result.getId(),
                        result.getRefundReference(),
                        admin.getId(),
                        request.resolution() == RefundManualReviewResolution.MARK_SUCCEEDED
                                ? AuditAction.REFUND_MARKED_MANUAL_SUCCESS
                                : request.resolution() == RefundManualReviewResolution.RETRY
                                        ? AuditAction.REFUND_RETRIED
                                        : request.resolution()
                                                        == RefundManualReviewResolution.REJECT
                                                ? AuditAction.REFUND_REJECTED
                                                : AuditAction.REFUND_FAILED,
                        before,
                        result.getStatus().name(),
                        LocalDateTime.now(clock)));
        return result;
    }

    private Refund terminal(Refund r, String code, String note) {
        LocalDateTime now = LocalDateTime.now(clock);
        r.setStatus(RefundStatus.FAILED);
        r.setManualReviewRequired(false);
        r.setLastFailureCode(code);
        r.setFailureReason(safe(note, "Refund marked failed after manual review"));
        r.setNextAttemptAt(null);
        r.setFailedAt(now);
        r.setUpdatedAt(now);
        return refunds.save(r);
    }

    private Refund retry(Refund r) {
        if (r.getAttemptCount() >= r.getMaxAttempts())
            throw new PaymentConflictException(
                    "Refund attempts are exhausted; resolve as success or failure");
        LocalDateTime now = LocalDateTime.now(clock);
        r.setStatus(RefundStatus.FAILED);
        r.setManualReviewRequired(false);
        r.setNextAttemptAt(now);
        r.setLastFailureCode(null);
        r.setFailureReason(null);
        r.setUpdatedAt(now);
        return refunds.save(r);
    }

    private Refund reject(Refund r, User admin) {
        if (r.getProviderRefundReference() != null)
            throw new PaymentConflictException(
                    "Refund with an external reference cannot be rejected");
        LocalDateTime now = LocalDateTime.now(clock);
        r.setStatus(RefundStatus.REJECTED);
        r.setManualReviewRequired(false);
        r.setRejectedBy(admin);
        r.setRejectedAt(now);
        r.setRejectionReasonCode("MANUAL_REVIEW_REJECTED");
        r.setRejectionNote(null);
        r.setNextAttemptAt(null);
        r.setUpdatedAt(now);
        return refunds.save(r);
    }

    private Refund lock(String ref) {
        return refunds.findByRefundReferenceForUpdate(ref.trim().toUpperCase())
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Refund not found with reference: " + ref));
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        String s = value.replaceAll("[\\p{Cntrl}]", "").trim();
        return s.substring(0, Math.min(500, s.length()));
    }
}
