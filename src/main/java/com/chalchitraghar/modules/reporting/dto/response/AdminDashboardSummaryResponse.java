package com.chalchitraghar.modules.reporting.dto.response;

import java.time.LocalDateTime;

public record AdminDashboardSummaryResponse(
        ReportingPeriodResponse period,
        AdminBookingKpiResponse bookings,
        AdminRevenueKpiResponse revenue,
        long registeredCustomers,
        long activeMovies,
        long runningShows,
        long scheduledShows,
        LocalDateTime generatedAt) {}
