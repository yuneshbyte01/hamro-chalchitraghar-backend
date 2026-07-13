package com.chalchitraghar.modules.notifications.event;

import java.time.LocalDateTime;

public record PaymentSucceededEvent(
        Long userId,
        Long paymentId,
        String paymentReference,
        String bookingReference,
        LocalDateTime occurredAt) {}
