package com.chalchitraghar.modules.reporting.dto.request;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public record ReportingDateRange(
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive,
        String timeZone) {

    public static ReportingDateRange of(LocalDate startDate, LocalDate endDate, Clock clock) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("startDate and endDate are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not be before startDate");
        }
        if (ChronoUnit.DAYS.between(startDate, endDate) + 1 > 366) {
            throw new IllegalArgumentException("Reporting date range must not exceed 366 days");
        }
        return new ReportingDateRange(
                startDate,
                endDate,
                startDate.atStartOfDay(),
                endDate.plusDays(1).atStartOfDay(),
                clock.getZone().getId());
    }
}
