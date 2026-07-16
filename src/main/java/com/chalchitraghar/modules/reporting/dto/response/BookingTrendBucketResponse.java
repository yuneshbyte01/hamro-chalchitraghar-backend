package com.chalchitraghar.modules.reporting.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record BookingTrendBucketResponse(
        LocalDate periodStart,
        LocalDate periodEnd,
        long totalBookings,
        List<BookingStatusCountResponse> statusBreakdown,
        long confirmedBookings,
        long cancelledBookings,
        long expiredBookings,
        BigDecimal confirmationRate,
        BigDecimal cancellationRate,
        BigDecimal expirationRate) {}
