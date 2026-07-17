package com.chalchitraghar.modules.reporting.comparison.dto;

import java.math.BigDecimal;

public record MetricComparisonResponse(
        BigDecimal currentValue,
        BigDecimal comparisonValue,
        BigDecimal absoluteChange,
        BigDecimal percentageChange,
        boolean comparable) {}
