package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;

public record CurrencyRefundAnalyticsResponse(
        String currency,
        BigDecimal refundedAmount,
        BigDecimal recognizedGrossPaymentAmount,
        BigDecimal refundAmountRate,
        BigDecimal averageRefundAmount) {}
