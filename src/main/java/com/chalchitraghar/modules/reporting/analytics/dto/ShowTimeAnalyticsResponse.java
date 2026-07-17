package com.chalchitraghar.modules.reporting.analytics.dto;

import com.chalchitraghar.modules.reporting.dto.response.CurrencyPerformanceAmountResponse;
import java.math.BigDecimal;
import java.util.List;

public record ShowTimeAnalyticsResponse(
        String bucket,
        long eligibleShowCount,
        long ticketsSold,
        long generatedSeatCount,
        BigDecimal occupancyPercentage,
        long confirmedBookingCount,
        BigDecimal averageAttendance,
        List<CurrencyPerformanceAmountResponse> revenueByCurrency) {}
