package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;

public record AnalyticsOverviewResponse(
        BigDecimal repeatCustomerRate,
        BigDecimal bookingAttemptConversionRate,
        BigDecimal cancellationRate,
        BigDecimal refundCountRate,
        BigDecimal refundAmountRate,
        BigDecimal averageSeatsPerConfirmedBooking,
        BigDecimal averageSuccessfulPaymentAmount,
        BigDecimal topMovieRevenueConcentration,
        String peakBookingHour,
        String peakBookingWeekday,
        PerformanceExtremeResponse bestMovie,
        PerformanceExtremeResponse bestHall,
        String bestShowTime,
        PerformanceExtremeResponse lowestMovie,
        PerformanceExtremeResponse lowestHall,
        long dataQualityWarningCount) {}
