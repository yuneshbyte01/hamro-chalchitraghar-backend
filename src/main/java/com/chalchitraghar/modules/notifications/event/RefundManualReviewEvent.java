package com.chalchitraghar.modules.notifications.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundManualReviewEvent(
        Long refundId,
        String refundReference,
        Long userId,
        Long actorUserId,
        String bookingReference,
        String paymentReference,
        BigDecimal amount,
        String currency,
        int attemptNumber,
        String failureCode,
        LocalDateTime occurredAt) {}
