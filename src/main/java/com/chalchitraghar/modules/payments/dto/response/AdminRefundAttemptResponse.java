package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.payments.enums.*;
import java.time.LocalDateTime;

public record AdminRefundAttemptResponse(
        int attemptNumber,
        RefundMethod method,
        PaymentProvider provider,
        RefundAttemptStatus status,
        LocalDateTime startedAt,
        LocalDateTime completedAt,
        String providerRefundReference,
        String providerStatus,
        String failureCode,
        String sanitizedFailureReason,
        String correlationId) {}
