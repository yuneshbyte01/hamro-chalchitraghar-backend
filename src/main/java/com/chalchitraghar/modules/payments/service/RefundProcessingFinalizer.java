package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.notifications.event.*;
import com.chalchitraghar.modules.payments.config.RefundProcessingProperties;
import com.chalchitraghar.modules.payments.entity.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.gateway.*;
import com.chalchitraghar.modules.payments.repository.*;
import com.chalchitraghar.shared.exception.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundProcessingFinalizer {
    private final RefundRepository refunds;
    private final RefundAttemptRepository attempts;
    private final PaymentRepository payments;
    private final PaymentLifecycleService paymentLifecycle;
    private final RefundProcessingProperties properties;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Transactional
    public Refund finalizeResult(Long refundId, int attemptNumber, RefundGatewayResult result) {
        Refund r =
                refunds.findById(refundId)
                        .orElseThrow(() -> new ResourceNotFoundException("Refund", refundId));
        r = refunds.findByRefundReferenceForUpdate(r.getRefundReference()).orElseThrow();
        if (r.getStatus() != RefundStatus.PROCESSING) return r;
        RefundAttempt a = attempts.findForUpdate(r.getId(), attemptNumber).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);
        r.setProviderRefundReference(safe(result.providerRefundReference(), 255));
        r.setProviderStatus(safe(result.providerStatus(), 100));
        a.setProviderRefundReference(r.getProviderRefundReference());
        a.setProviderStatus(r.getProviderStatus());
        switch (result.outcome()) {
            case SUCCEEDED -> succeed(r, a, now);
            case FAILED_RETRYABLE -> fail(r, a, result, now, true);
            case FAILED_TERMINAL -> fail(r, a, result, now, false);
            case UNKNOWN, MANUAL_REVIEW -> review(r, a, result, now);
        }
        r.setClaimedAt(null);
        r.setClaimedBy(null);
        r.setUpdatedAt(now);
        a.setUpdatedAt(now);
        if (r.getStatus() == RefundStatus.SUCCEEDED)
            events.publishEvent(
                    new RefundSucceededEvent(
                            r.getId(),
                            r.getRefundReference(),
                            r.getBooking().getUser().getId(),
                            null,
                            r.getBooking().getBookingReference(),
                            r.getPayment().getPaymentReference(),
                            r.getAmount(),
                            r.getCurrency(),
                            attemptNumber,
                            now));
        else if (r.getStatus() == RefundStatus.MANUAL_REVIEW)
            events.publishEvent(
                    new RefundManualReviewEvent(
                            r.getId(),
                            r.getRefundReference(),
                            r.getBooking().getUser().getId(),
                            null,
                            r.getBooking().getBookingReference(),
                            r.getPayment().getPaymentReference(),
                            r.getAmount(),
                            r.getCurrency(),
                            attemptNumber,
                            r.getLastFailureCode(),
                            now));
        return refunds.save(r);
    }

    private void succeed(Refund r, RefundAttempt a, LocalDateTime now) {
        Payment p =
                payments.findByPaymentReferenceForUpdate(r.getPayment().getPaymentReference())
                        .orElseThrow();
        if (r.getAmount().compareTo(p.getAmount()) != 0 || !r.getCurrency().equals(p.getCurrency()))
            throw new PaymentConflictException(
                    "Refund does not match full payment amount and currency");
        if (p.getStatus() == PaymentStatus.SUCCESS)
            paymentLifecycle.transition(p, PaymentStatus.REFUNDED);
        else if (p.getStatus() != PaymentStatus.REFUNDED)
            throw new PaymentConflictException("Payment is not refundable");
        r.setStatus(RefundStatus.SUCCEEDED);
        r.setProcessedAt(now);
        r.setCompletedAt(now);
        r.setNextAttemptAt(null);
        r.setFailureReason(null);
        r.setLastFailureCode(null);
        a.setStatus(RefundAttemptStatus.SUCCEEDED);
        a.setCompletedAt(now);
    }

    private void fail(
            Refund r,
            RefundAttempt a,
            RefundGatewayResult x,
            LocalDateTime now,
            boolean retryable) {
        r.setStatus(RefundStatus.FAILED);
        r.setFailedAt(now);
        r.setLastFailureCode(code(x));
        r.setFailureReason(safe(x.sanitizedMessage(), 500));
        boolean retry = retryable && r.getAttemptCount() < r.getMaxAttempts();
        r.setNextAttemptAt(retry ? now.plus(retryDelay(r.getAttemptCount())) : null);
        if (retryable && !retry) {
            r.setStatus(RefundStatus.MANUAL_REVIEW);
            r.setManualReviewRequired(true);
        }
        a.setStatus(RefundAttemptStatus.FAILED);
        a.setCompletedAt(now);
        a.setFailureCode(code(x));
        a.setFailureReason(r.getFailureReason());
    }

    private void review(Refund r, RefundAttempt a, RefundGatewayResult x, LocalDateTime now) {
        r.setStatus(RefundStatus.MANUAL_REVIEW);
        r.setManualReviewRequired(true);
        r.setNextAttemptAt(null);
        r.setLastFailureCode(code(x));
        r.setFailureReason(safe(x.sanitizedMessage(), 500));
        a.setStatus(RefundAttemptStatus.UNKNOWN);
        a.setCompletedAt(now);
        a.setFailureCode(code(x));
        a.setFailureReason(r.getFailureReason());
    }

    private Duration retryDelay(int n) {
        double seconds =
                properties.getInitialRetryDelay().toSeconds()
                        * Math.pow(properties.getRetryMultiplier(), Math.max(0, n - 1));
        return Duration.ofSeconds(
                Math.min((long) seconds, properties.getMaxRetryDelay().toSeconds()));
    }

    private String code(RefundGatewayResult x) {
        return safe(x.failureCode() == null ? "PROVIDER_STATUS_UNKNOWN" : x.failureCode(), 100);
    }

    private String safe(String v, int max) {
        if (v == null || v.isBlank()) return null;
        String s = v.replaceAll("[\\p{Cntrl}]", "").trim();
        return s.substring(0, Math.min(max, s.length()));
    }
}
