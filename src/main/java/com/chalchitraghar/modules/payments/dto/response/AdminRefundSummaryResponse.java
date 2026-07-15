package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.payments.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRefundSummaryResponse(
        String refundReference,
        String bookingReference,
        String paymentReference,
        Long customerId,
        String customerName,
        String customerEmail,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        RefundReason reason,
        RefundType type,
        RefundMethod method,
        LocalDateTime requestedAt) {}
