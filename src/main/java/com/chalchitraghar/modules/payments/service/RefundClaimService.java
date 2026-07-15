package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.notifications.event.RefundProcessingStartedEvent;
import com.chalchitraghar.modules.payments.config.RefundProcessingProperties;
import com.chalchitraghar.modules.payments.entity.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.gateway.RefundExecutionCommand;
import com.chalchitraghar.modules.payments.repository.*;
import com.chalchitraghar.shared.exception.*;
import java.time.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundClaimService {
    private final RefundRepository refunds;
    private final RefundAttemptRepository attempts;
    private final RefundProcessingProperties properties;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    @Transactional
    public RefundProcessingClaim claim(String reference, boolean explicitRetry) {
        Refund r =
                refunds.findByRefundReferenceForUpdate(reference.trim().toUpperCase())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Refund not found with reference: " + reference));
        LocalDateTime now = LocalDateTime.now(clock);
        boolean dueFailure =
                r.getStatus() == RefundStatus.FAILED
                        && (explicitRetry
                                || r.getNextAttemptAt() == null
                                || !r.getNextAttemptAt().isAfter(now));
        if (r.getStatus() != RefundStatus.APPROVED && !dueFailure)
            throw new PaymentConflictException("Refund is not eligible for processing");
        if (r.getAttemptCount() >= r.getMaxAttempts())
            throw new PaymentConflictException("Refund processing attempts are exhausted");
        int number = r.getAttemptCount() + 1;
        r.setAttemptCount(number);
        r.setStatus(RefundStatus.PROCESSING);
        r.setLastAttemptAt(now);
        r.setProcessingStartedAt(now);
        r.setClaimedAt(now);
        r.setClaimedBy(properties.getWorkerId());
        r.setNextAttemptAt(null);
        r.setManualReviewRequired(false);
        r.setUpdatedAt(now);
        RefundAttempt a =
                RefundAttempt.builder()
                        .refund(r)
                        .attemptNumber(number)
                        .method(r.getMethod())
                        .provider(r.getProvider())
                        .status(RefundAttemptStatus.STARTED)
                        .startedAt(now)
                        .correlationId(UUID.randomUUID().toString())
                        .build();
        a.setCreatedAt(now);
        a.setUpdatedAt(now);
        attempts.save(a);
        refunds.save(r);
        events.publishEvent(
                new RefundProcessingStartedEvent(
                        r.getId(),
                        r.getRefundReference(),
                        r.getBooking().getUser().getId(),
                        null,
                        r.getBooking().getBookingReference(),
                        r.getPayment().getPaymentReference(),
                        r.getAmount(),
                        r.getCurrency(),
                        number,
                        now));
        return new RefundProcessingClaim(
                r.getId(),
                number,
                new RefundExecutionCommand(
                        r.getRefundReference(),
                        "REFUND:" + r.getId(),
                        r.getPayment().getPaymentReference(),
                        r.getBooking().getBookingReference(),
                        r.getAmount(),
                        r.getCurrency(),
                        r.getProvider(),
                        r.getMethod(),
                        a.getCorrelationId()));
    }
}
