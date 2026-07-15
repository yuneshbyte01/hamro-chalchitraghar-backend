package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.payments.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerRefundDetailResponse(
        String refundReference,
        String bookingReference,
        String paymentReference,
        String movieName,
        String hallName,
        LocalDateTime showDateTime,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        RefundReason reason,
        RefundType type,
        RefundMethod method,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        LocalDateTime processedAt,
        String safeStatusMessage) {}
