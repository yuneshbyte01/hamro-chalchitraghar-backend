package com.chalchitraghar.modules.reporting.export;

import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.enums.ReportExportFormat;
import com.chalchitraghar.modules.shows.enums.ShowStatus;

public interface ReportingExportService {
    ReportExportResult revenue(
            ReportingDateRange range,
            String currency,
            ReportGrouping grouping,
            ReportExportFormat format);

    ReportExportResult bookings(
            ReportingDateRange range, ReportGrouping grouping, ReportExportFormat format);

    ReportExportResult occupancy(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            OccupancySort sort,
            ReportSortDirection direction,
            ReportExportFormat format);

    ReportExportResult movies(
            ReportingDateRange range,
            String currency,
            MoviePerformanceSort sort,
            ReportSortDirection direction,
            ReportExportFormat format);

    ReportExportResult halls(
            ReportingDateRange range,
            String currency,
            HallPerformanceSort sort,
            ReportSortDirection direction,
            ReportExportFormat format);

    ReportExportResult shows(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            String currency,
            ShowPerformanceSort sort,
            ReportSortDirection direction,
            ReportExportFormat format);
}
