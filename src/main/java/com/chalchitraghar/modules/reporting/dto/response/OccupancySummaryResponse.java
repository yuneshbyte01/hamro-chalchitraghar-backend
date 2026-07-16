package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.shared.response.PageResponse;
import java.math.BigDecimal;

public record OccupancySummaryResponse(
        ReportingPeriodResponse period,
        long totalEligibleShows,
        long totalGeneratedSeats,
        long totalSoldSeats,
        BigDecimal overallOccupancyPercentage,
        PageResponse<ShowOccupancyResponse> shows) {}
