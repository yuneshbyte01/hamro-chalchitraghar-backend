package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;
import java.util.Map;

public record SeatUtilizationRowResponse(
        String dimensionType,
        String dimension,
        long generatedSeatCount,
        long soldSeatCount,
        long unsoldSeatCount,
        BigDecimal utilizationPercentage,
        Map<String, BigDecimal> soldValueByCurrency,
        Map<String, BigDecimal> averageUnitPriceByCurrency) {}
