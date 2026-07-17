package com.chalchitraghar.modules.reporting.analytics.dto;

import java.math.BigDecimal;

public record ConcentrationEntityResponse(
        long id,
        String name,
        BigDecimal revenue,
        BigDecimal percentageOfTotal,
        BigDecimal cumulativePercentage) {}
