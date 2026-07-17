package com.chalchitraghar.modules.reporting.comparison.dto;

import com.chalchitraghar.modules.reporting.dto.response.HallPerformanceResponse;

public record HallComparisonResponse(
        HallPerformanceResponse performance,
        Integer revenueRank,
        int ticketsSoldRank,
        int occupancyRank) {}
