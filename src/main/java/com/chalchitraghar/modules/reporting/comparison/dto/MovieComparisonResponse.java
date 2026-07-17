package com.chalchitraghar.modules.reporting.comparison.dto;

import com.chalchitraghar.modules.reporting.dto.response.MoviePerformanceResponse;

public record MovieComparisonResponse(
        MoviePerformanceResponse performance,
        Integer revenueRank,
        int ticketsSoldRank,
        int occupancyRank) {}
