package com.chalchitraghar.modules.reporting.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class ReportingRateCalculator {
    public BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) return BigDecimal.ZERO.setScale(2);
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }
}
