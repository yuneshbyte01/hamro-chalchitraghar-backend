package com.chalchitraghar.modules.reporting.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueTrendBucketResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        String currency,
        BigDecimal grossRevenue,
        BigDecimal refundAmount,
        BigDecimal netRevenue,
        long successfulPaymentCount,
        long successfulRefundCount,
        BigDecimal averageSuccessfulPaymentAmount) {}
