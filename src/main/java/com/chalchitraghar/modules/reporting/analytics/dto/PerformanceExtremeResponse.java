package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;

public record PerformanceExtremeResponse(
        Long id, String name, BigDecimal value, long eligibleShows) {}
