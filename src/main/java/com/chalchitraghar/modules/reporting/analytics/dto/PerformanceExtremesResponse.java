package com.chalchitraghar.modules.reporting.analytics.dto;

import java.util.Map;

public record PerformanceExtremesResponse(
        String currency,
        int minimumShows,
        Map<String, PerformanceExtremeResponse> best,
        Map<String, PerformanceExtremeResponse> lowest) {}
