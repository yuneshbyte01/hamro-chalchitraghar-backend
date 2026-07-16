package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReportingPeriodResponse(
        LocalDate startDate,
        LocalDate endDate,
        LocalDateTime startInclusive,
        LocalDateTime endExclusive,
        String timeZone) {
    public static ReportingPeriodResponse from(ReportingDateRange range) {
        return new ReportingPeriodResponse(
                range.startDate(),
                range.endDate(),
                range.startInclusive(),
                range.endExclusive(),
                range.timeZone());
    }
}
