package com.chalchitraghar.modules.reporting.analytics.dto;

import com.chalchitraghar.modules.reporting.analytics.ConcentrationDimension;
import java.math.BigDecimal;
import java.util.List;

public record RevenueConcentrationResponse(
        String currency,
        ConcentrationDimension dimension,
        BigDecimal totalRecognizedGrossRevenue,
        List<ConcentrationEntityResponse> topEntities,
        BigDecimal remainingRevenue,
        BigDecimal remainingPercentage) {}
