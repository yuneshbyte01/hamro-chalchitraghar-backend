package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.payments.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerRefundSummaryResponse(
        String refundReference,
        String bookingReference,
        String paymentReference,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        RefundReason reason,
        RefundType type,
        LocalDateTime requestedAt) {}
