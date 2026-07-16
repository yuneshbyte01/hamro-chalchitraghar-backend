package com.chalchitraghar.modules.reporting.dto.response;

import java.math.BigDecimal;

public record CurrencyPerformanceAmountResponse(
        String currency,
        BigDecimal grossRevenue,
        BigDecimal refundAmount,
        BigDecimal netRevenue,
        BigDecimal averageRevenuePerShow) {}
