package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;

public record BookingPatternBucketResponse(
        String label,
        long bookingCount,
        long confirmedBookingCount,
        long cancelledBookingCount,
        BigDecimal conversionRate,
        BigDecimal percentageOfTotal) {}
