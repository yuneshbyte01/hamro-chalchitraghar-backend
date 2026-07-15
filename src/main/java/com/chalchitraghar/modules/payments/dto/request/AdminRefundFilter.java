package com.chalchitraghar.modules.payments.dto.request;

import com.chalchitraghar.modules.payments.enums.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRefundFilter(
        String refundReference,
        String paymentReference,
        String bookingReference,
        Long customerId,
        String customerEmail,
        RefundStatus status,
        RefundReason reason,
        RefundType type,
        RefundMethod method,
        PaymentProvider provider,
        LocalDateTime requestedFrom,
        LocalDateTime requestedTo,
        BigDecimal amountFrom,
        BigDecimal amountTo) {}
