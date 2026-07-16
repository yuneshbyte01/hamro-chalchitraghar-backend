package com.chalchitraghar.modules.payments.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.notifications.event.RefundSucceededEvent;
import com.chalchitraghar.modules.payments.dto.request.AdminManualRefundSuccessRequest;
import com.chalchitraghar.modules.payments.dto.response.AdminRefundAttemptResponse;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.entity.RefundAttempt;
import com.chalchitraghar.modules.payments.enums.PaymentStatus;
import com.chalchitraghar.modules.payments.enums.RefundAttemptStatus;
import com.chalchitraghar.modules.payments.enums.RefundMethod;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.modules.payments.repository.RefundAttemptRepository;
import com.chalchitraghar.modules.payments.repository.RefundRepository;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.PaymentConflictException;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RefundOperationsService {
    private final RefundRepository refunds;
    private final RefundAttemptRepository attempts;
    private final PaymentRepository payments;
    private final PaymentLifecycleService paymentLifecycle;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Transactional
    public Refund markManualSuccess(
            String reference, AdminManualRefundSuccessRequest request, User admin) {
        Refund r = lock(reference);
        String external = safe(request.externalReference(), 255);
        if (r.getStatus() == RefundStatus.SUCCEEDED) {
            if (Objects.equals(r.getProviderRefundReference(), external)) return r;
            throw new PaymentConflictException(
                    "Refund already completed with a different external reference");
        }
        if (r.getMethod() != RefundMethod.MANUAL
                || !Set.of(RefundStatus.APPROVED, RefundStatus.FAILED, RefundStatus.MANUAL_REVIEW)
                        .contains(r.getStatus()))
            throw new PaymentConflictException("Refund is not eligible for manual completion");
        Payment p =
                payments.findByPaymentReferenceForUpdate(r.getPayment().getPaymentReference())
                        .orElseThrow();
        if (p.getStatus() != PaymentStatus.SUCCESS
                || r.getAmount().compareTo(p.getAmount()) != 0
                || !r.getCurrency().equals(p.getCurrency()))
            throw new PaymentConflictException("Payment does not match an unsettled full refund");
        LocalDateTime now = LocalDateTime.now(clock);
        RefundAttempt a = existingManualAttempt(r);
        int number;
        if (a == null) {
            number = r.getAttemptCount() + 1;
            if (number > r.getMaxAttempts())
                throw new PaymentConflictException("Refund processing attempts are exhausted");
            r.setAttemptCount(number);
            a =
                    RefundAttempt.builder()
                            .refund(r)
                            .attemptNumber(number)
                            .method(r.getMethod())
                            .provider(r.getProvider())
                            .startedAt(now)
                            .correlationId("ADMIN:" + admin.getId())
                            .build();
            a.setCreatedAt(now);
        } else number = a.getAttemptNumber();
        a.setStatus(RefundAttemptStatus.SUCCEEDED);
        a.setCompletedAt(now);
        a.setProviderRefundReference(external);
        a.setProviderStatus("MANUALLY_CONFIRMED");
        a.setFailureCode(null);
        a.setFailureReason(null);
        a.setUpdatedAt(now);
        attempts.save(a);
        paymentLifecycle.transition(p, PaymentStatus.REFUNDED);
        r.setStatus(RefundStatus.SUCCEEDED);
        r.setProcessedAt(now);
        r.setCompletedAt(now);
        r.setProviderRefundReference(external);
        r.setProviderStatus("MANUALLY_CONFIRMED");
        r.setManualReviewRequired(false);
        r.setNextAttemptAt(null);
        r.setClaimedAt(null);
        r.setClaimedBy(null);
        r.setFailureReason(null);
        r.setLastFailureCode(null);
        r.setUpdatedAt(now);
        Refund saved = refunds.save(r);
        events.publishEvent(
                new RefundSucceededEvent(
                        r.getId(),
                        r.getRefundReference(),
                        r.getBooking().getUser().getId(),
                        admin.getId(),
                        r.getBooking().getBookingReference(),
                        p.getPaymentReference(),
                        r.getAmount(),
                        r.getCurrency(),
                        number,
                        now));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<AdminRefundAttemptResponse> attempts(String reference) {
        Refund r =
                refunds.findByRefundReference(reference.trim().toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        return attempts.findByRefundIdOrderByAttemptNumberAsc(r.getId()).stream()
                .map(
                        a ->
                                new AdminRefundAttemptResponse(
                                        a.getAttemptNumber(),
                                        a.getMethod(),
                                        a.getProvider(),
                                        a.getStatus(),
                                        a.getStartedAt(),
                                        a.getCompletedAt(),
                                        a.getProviderRefundReference(),
                                        a.getProviderStatus(),
                                        a.getFailureCode(),
                                        a.getFailureReason(),
                                        a.getCorrelationId()))
                .toList();
    }

    private Refund lock(String ref) {
        return refunds.findByRefundReferenceForUpdate(ref.trim().toUpperCase())
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Refund not found with reference: " + ref));
    }

    private RefundAttempt existingManualAttempt(Refund refund) {
        if (refund.getStatus() != RefundStatus.MANUAL_REVIEW || refund.getAttemptCount() == 0)
            return null;
        return attempts.findForUpdate(refund.getId(), refund.getAttemptCount())
                .filter(a -> a.getStatus() == RefundAttemptStatus.UNKNOWN)
                .orElse(null);
    }

    private String safe(String v, int max) {
        String s = v.replaceAll("[\\p{Cntrl}]", "").trim();
        if (s.isBlank()) throw new IllegalArgumentException("External reference is required");
        return s.substring(0, Math.min(max, s.length()));
    }
}
