package com.chalchitraghar.modules.reporting.dto.response;

import java.math.BigDecimal;

public record CurrencyRevenueKpiResponse(
        String currency,
        BigDecimal grossRevenue,
        BigDecimal refundAmount,
        BigDecimal netRevenue,
        long successfulPaymentCount,
        long successfulRefundCount,
        BigDecimal averageSuccessfulPaymentAmount) {}
