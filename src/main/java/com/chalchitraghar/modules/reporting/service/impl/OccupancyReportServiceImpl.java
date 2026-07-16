package com.chalchitraghar.modules.reporting.service.impl;

import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.repository.ReportingAnalyticsRepository;
import com.chalchitraghar.modules.reporting.service.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.shared.response.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OccupancyReportServiceImpl implements OccupancyReportService {
    private final ReportingAnalyticsRepository repository;
    private final ReportingRateCalculator rates;

    @Override
    public OccupancySummaryResponse getReport(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            OccupancySort sort,
            ReportSortDirection direction,
            int page,
            int size) {
        validatePage(page, size);
        OccupancySort effectiveSort = sort == null ? OccupancySort.SHOW_DATE : sort;
        ReportSortDirection effectiveDirection =
                direction == null ? ReportSortDirection.DESC : direction;
        var totals =
                repository.occupancyTotals(
                        range.startDate(), range.endDate(), movieId, hallId, status);
        var rows =
                repository.occupancyPage(
                        range.startDate(),
                        range.endDate(),
                        movieId,
                        hallId,
                        status,
                        effectiveSort,
                        effectiveDirection,
                        page,
                        size);
        List<ShowOccupancyResponse> content =
                rows.content().stream()
                        .map(
                                row ->
                                        new ShowOccupancyResponse(
                                                row.showId(),
                                                row.movieId(),
                                                row.movieTitle(),
                                                row.hallId(),
                                                row.hallName(),
                                                row.showDate(),
                                                row.showTime(),
                                                row.status(),
                                                row.capacity(),
                                                row.sold(),
                                                Math.max(0, row.capacity() - row.sold()),
                                                rates.percentage(row.sold(), row.capacity()),
                                                row.capacity() > 0))
                        .toList();
        return new OccupancySummaryResponse(
                ReportingPeriodResponse.from(range),
                totals.shows(),
                totals.capacity(),
                totals.sold(),
                rates.percentage(totals.sold(), totals.capacity()),
                page(content, page, size, rows.total()));
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException(
                    "page must be non-negative and size must be between 1 and 100");
    }

    private <T> PageResponse<T> page(List<T> content, int page, int size, long total) {
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, pages, page + 1 >= pages);
    }
}
