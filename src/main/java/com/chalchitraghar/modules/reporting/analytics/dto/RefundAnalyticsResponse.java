package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;
import java.util.*;

public record RefundAnalyticsResponse(
        long successfulRefundCount,
        long failedRefundCount,
        long pendingRefundCount,
        long refundedBookingCount,
        long recognizedPaidBookingCount,
        BigDecimal refundCountRate,
        String mostCommonRefundReason,
        Map<String, Long> methodBreakdown,
        Map<String, Long> providerBreakdown,
        List<CurrencyRefundAnalyticsResponse> currencies) {}
