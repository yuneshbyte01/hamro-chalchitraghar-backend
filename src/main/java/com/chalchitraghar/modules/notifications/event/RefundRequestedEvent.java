package com.chalchitraghar.modules.notifications.event;

import com.chalchitraghar.modules.payments.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RefundRequestedEvent(
        Long refundId,
        String refundReference,
        Long userId,
        Long actorUserId,
        String bookingReference,
        String paymentReference,
        BigDecimal amount,
        String currency,
        RefundReason reason,
        RefundStatus status,
        LocalDateTime occurredAt) {}
