package com.chalchitraghar.modules.payments.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminRefundSummaryReport(
        LocalDateTime from,
        LocalDateTime to,
        long totalRefundIntents,
        List<RefundBreakdownItem> statusBreakdown,
        List<RefundBreakdownItem> reasonBreakdown,
        List<RefundBreakdownItem> methodBreakdown,
        List<RefundBreakdownItem> providerBreakdown,
        long retryCount,
        long exhaustedCount,
        double successRate,
        double failureRate,
        long uniqueBookings,
        long uniquePayments) {}
