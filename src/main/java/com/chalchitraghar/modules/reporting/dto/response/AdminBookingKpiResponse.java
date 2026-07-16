package com.chalchitraghar.modules.reporting.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record AdminBookingKpiResponse(
        ReportingPeriodResponse period,
        long totalBookings,
        long confirmedBookings,
        long cancelledBookings,
        long expiredBookings,
        List<BookingStatusCountResponse> statusBreakdown,
        BigDecimal confirmationRate,
        BigDecimal cancellationRate,
        BigDecimal expirationRate) {}
