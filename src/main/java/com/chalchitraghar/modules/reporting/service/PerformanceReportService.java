package com.chalchitraghar.modules.reporting.service;

import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.shared.response.PageResponse;

public interface PerformanceReportService {
    PageResponse<MoviePerformanceResponse> movies(
            ReportingDateRange range,
            String currency,
            MoviePerformanceSort sort,
            ReportSortDirection direction,
            int page,
            int size);

    PageResponse<HallPerformanceResponse> halls(
            ReportingDateRange range,
            String currency,
            HallPerformanceSort sort,
            ReportSortDirection direction,
            int page,
            int size);

    PageResponse<ShowPerformanceResponse> shows(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            String currency,
            ShowPerformanceSort sort,
            ReportSortDirection direction,
            int page,
            int size);
}
