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
        BigDecimal amountTo,
        String currency,
        LocalDateTime approvedFrom,
        LocalDateTime approvedTo,
        LocalDateTime processedFrom,
        LocalDateTime processedTo,
        LocalDateTime failedFrom,
        LocalDateTime failedTo,
        Integer attemptCountFrom,
        Integer attemptCountTo,
        Boolean manualReviewOnly,
        Boolean failedOnly,
        Boolean retryEligibleOnly,
        Boolean exhaustedOnly,
        String providerRefundReference,
        LocalDateTime createdFrom,
        LocalDateTime createdTo,
        LocalDateTime evaluationTime) {}
