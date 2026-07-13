package com.chalchitraghar.modules.payments.dto.response;

import java.math.BigDecimal;

public record PaymentStatisticsResponse(
        long total,
        long successful,
        long pending,
        long failed,
        long expired,
        long manualReview,
        double successRate,
        BigDecimal averageAmount,
        BigDecimal totalRevenue) {}
