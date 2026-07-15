package com.chalchitraghar.modules.notifications.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundProcessingStartedEvent(
        Long refundId,
        String refundReference,
        Long userId,
        Long actorUserId,
        String bookingReference,
        String paymentReference,
        BigDecimal amount,
        String currency,
        int attemptNumber,
        LocalDateTime occurredAt) {}
