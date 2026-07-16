package com.chalchitraghar.modules.reporting.service;

import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.OccupancySummaryResponse;
import com.chalchitraghar.modules.shows.enums.ShowStatus;

public interface OccupancyReportService {
    OccupancySummaryResponse getReport(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            OccupancySort sort,
            ReportSortDirection direction,
            int page,
            int size);
}
