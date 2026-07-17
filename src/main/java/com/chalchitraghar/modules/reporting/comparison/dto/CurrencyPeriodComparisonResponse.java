package com.chalchitraghar.modules.reporting.comparison.dto;

public record CurrencyPeriodComparisonResponse(
        String currency,
        MetricComparisonResponse grossRevenue,
        MetricComparisonResponse refundAmount,
        MetricComparisonResponse netRevenue,
        MetricComparisonResponse successfulPaymentCount,
        MetricComparisonResponse successfulRefundCount) {}
