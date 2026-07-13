package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.PaymentStatus;
import com.chalchitraghar.shared.exception.PaymentConflictException;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentLifecycleService {
    public static final Set<PaymentStatus> ACTIVE =
            Set.of(PaymentStatus.CREATED, PaymentStatus.PENDING);
    private static final Map<PaymentStatus, Set<PaymentStatus>> ALLOWED =
            Map.of(
                    PaymentStatus.CREATED,
                            Set.of(
                                    PaymentStatus.PENDING,
                                    PaymentStatus.CANCELLED,
                                    PaymentStatus.EXPIRED),
                    PaymentStatus.PENDING,
                            Set.of(
                                    PaymentStatus.SUCCESS,
                                    PaymentStatus.FAILED,
                                    PaymentStatus.EXPIRED,
                                    PaymentStatus.CANCELLED));
    private final Clock clock;

    public boolean reconcileExpiry(Payment p) {
        if (ACTIVE.contains(p.getStatus())
                && p.getExpiresAt() != null
                && !LocalDateTime.now(clock).isBefore(p.getExpiresAt())) {
            transition(p, PaymentStatus.EXPIRED);
            return true;
        }
        return false;
    }

    public void transition(Payment p, PaymentStatus target) {
        if (!ALLOWED.getOrDefault(p.getStatus(), Set.of()).contains(target))
            throw new PaymentConflictException(
                    "Payment cannot transition from " + p.getStatus() + " to " + target);
        LocalDateTime now = LocalDateTime.now(clock);
        p.setStatus(target);
        switch (target) {
            case SUCCESS -> p.setCompletedAt(now);
            case FAILED -> p.setFailedAt(now);
            case EXPIRED -> p.setExpiredAt(now);
            case CANCELLED -> p.setCancelledAt(now);
            default -> {}
        }
    }
}
