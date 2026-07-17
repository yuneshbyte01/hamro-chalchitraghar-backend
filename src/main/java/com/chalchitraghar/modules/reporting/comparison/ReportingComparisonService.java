package com.chalchitraghar.modules.reporting.comparison;

import com.chalchitraghar.modules.reporting.comparison.dto.*;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.enums.ComparisonMode;
import java.time.LocalDate;
import java.util.List;

public interface ReportingComparisonService {
    PeriodComparisonResponse periods(
            ReportingDateRange current,
            ComparisonMode mode,
            LocalDate previousStart,
            LocalDate previousEnd,
            String currency);

    List<MovieComparisonResponse> movies(ReportingDateRange range, List<Long> ids, String currency);

    List<HallComparisonResponse> halls(ReportingDateRange range, List<Long> ids, String currency);
}
