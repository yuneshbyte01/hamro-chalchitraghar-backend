package com.chalchitraghar.modules.reporting.export;

import com.chalchitraghar.modules.reporting.config.ReportingOperationsProperties;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.enums.ReportExportFormat;
import com.chalchitraghar.modules.reporting.service.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.shared.response.PageResponse;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.*;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportingExportServiceImpl implements ReportingExportService {
    private static final int PAGE_SIZE = 100;
    private final RevenueReportService revenue;
    private final BookingReportService bookings;
    private final OccupancyReportService occupancy;
    private final PerformanceReportService performance;
    private final ReportExportFactory exporters;
    private final ReportingOperationsProperties properties;
    private final MeterRegistry metrics;

    public ReportExportResult revenue(
            ReportingDateRange range,
            String currency,
            ReportGrouping grouping,
            ReportExportFormat format) {
        var report = revenue.getDetailedReport(range, currency, grouping);
        var rows =
                report.trends().stream()
                        .map(
                                v ->
                                        List.<Object>of(
                                                v.periodStart(),
                                                v.periodEnd(),
                                                v.currency(),
                                                v.grossRevenue(),
                                                v.refundAmount(),
                                                v.netRevenue(),
                                                v.successfulPaymentCount(),
                                                v.successfulRefundCount(),
                                                v.averageSuccessfulPaymentAmount()))
                        .toList();
        return export(
                format,
                "revenue-report-" + dates(range),
                List.of(
                        "periodStart",
                        "periodEnd",
                        "currency",
                        "grossRevenue",
                        "refundAmount",
                        "netRevenue",
                        "successfulPaymentCount",
                        "successfulRefundCount",
                        "averageSuccessfulPaymentAmount"),
                rows);
    }

    public ReportExportResult bookings(
            ReportingDateRange range, ReportGrouping grouping, ReportExportFormat format) {
        var rows =
                bookings.getDetailedReport(range, grouping).trends().stream()
                        .map(
                                v ->
                                        List.<Object>of(
                                                v.periodStart(),
                                                v.periodEnd(),
                                                v.totalBookings(),
                                                v.confirmedBookings(),
                                                v.cancelledBookings(),
                                                v.expiredBookings(),
                                                v.confirmationRate(),
                                                v.cancellationRate(),
                                                v.expirationRate()))
                        .toList();
        return export(
                format,
                "booking-report-" + dates(range),
                List.of(
                        "periodStart",
                        "periodEnd",
                        "totalBookings",
                        "confirmedBookings",
                        "cancelledBookings",
                        "expiredBookings",
                        "confirmationRate",
                        "cancellationRate",
                        "expirationRate"),
                rows);
    }

    public ReportExportResult occupancy(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            OccupancySort sort,
            ReportSortDirection direction,
            ReportExportFormat format) {
        var all =
                pages(
                        page ->
                                occupancy
                                        .getReport(
                                                range, movieId, hallId, status, sort, direction,
                                                page, PAGE_SIZE)
                                        .shows());
        var rows =
                all.stream()
                        .map(
                                v ->
                                        List.<Object>of(
                                                v.showId(),
                                                v.movieId(),
                                                v.movieTitle(),
                                                v.hallId(),
                                                v.hallName(),
                                                v.showDate(),
                                                v.showTime(),
                                                v.showStatus(),
                                                v.generatedSeatCount(),
                                                v.soldSeatCount(),
                                                v.availableSeatCount(),
                                                v.occupancyPercentage(),
                                                v.measurable()))
                        .toList();
        return export(
                format,
                "occupancy-report-" + dates(range),
                List.of(
                        "showId",
                        "movieId",
                        "movieTitle",
                        "hallId",
                        "hallName",
                        "showDate",
                        "showTime",
                        "showStatus",
                        "generatedSeatCount",
                        "soldSeatCount",
                        "availableSeatCount",
                        "occupancyPercentage",
                        "measurable"),
                rows);
    }

    public ReportExportResult movies(
            ReportingDateRange range,
            String currency,
            MoviePerformanceSort sort,
            ReportSortDirection direction,
            ReportExportFormat format) {
        var all =
                pages(
                        page ->
                                performance.movies(
                                        range, currency, sort, direction, page, PAGE_SIZE));
        List<List<Object>> rows = new ArrayList<>();
        all.forEach(
                v ->
                        currencies(v.revenueByCurrency())
                                .forEach(
                                        m ->
                                                rows.add(
                                                        List.of(
                                                                v.movieId(),
                                                                v.movieTitle(),
                                                                v.movieStatus(),
                                                                v.showCount(),
                                                                v.confirmedBookingCount(),
                                                                v.ticketsSold(),
                                                                v.generatedSeatCount(),
                                                                v.occupancyPercentage(),
                                                                v.averageAttendancePerShow(),
                                                                m.currency(),
                                                                m.grossRevenue(),
                                                                m.refundAmount(),
                                                                m.netRevenue(),
                                                                m.averageRevenuePerShow()))));
        return export(
                format,
                "movie-performance-" + dates(range),
                performanceHeaders("movieId", "movieTitle", "movieStatus"),
                rows);
    }

    public ReportExportResult halls(
            ReportingDateRange range,
            String currency,
            HallPerformanceSort sort,
            ReportSortDirection direction,
            ReportExportFormat format) {
        var all =
                pages(page -> performance.halls(range, currency, sort, direction, page, PAGE_SIZE));
        List<List<Object>> rows = new ArrayList<>();
        all.forEach(
                v ->
                        currencies(v.revenueByCurrency())
                                .forEach(
                                        m ->
                                                rows.add(
                                                        List.of(
                                                                v.hallId(),
                                                                v.hallName(),
                                                                v.hallStatus(),
                                                                v.showCount(),
                                                                v.confirmedBookingCount(),
                                                                v.ticketsSold(),
                                                                v.generatedSeatCount(),
                                                                v.occupancyPercentage(),
                                                                v.averageAttendancePerShow(),
                                                                m.currency(),
                                                                m.grossRevenue(),
                                                                m.refundAmount(),
                                                                m.netRevenue(),
                                                                m.averageRevenuePerShow()))));
        return export(
                format,
                "hall-performance-" + dates(range),
                performanceHeaders("hallId", "hallName", "hallStatus"),
                rows);
    }

    public ReportExportResult shows(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            String currency,
            ShowPerformanceSort sort,
            ReportSortDirection direction,
            ReportExportFormat format) {
        var all =
                pages(
                        page ->
                                performance.shows(
                                        range, movieId, hallId, status, currency, sort, direction,
                                        page, PAGE_SIZE));
        List<List<Object>> rows = new ArrayList<>();
        all.forEach(
                v ->
                        currencies(v.revenueByCurrency())
                                .forEach(
                                        m ->
                                                rows.add(
                                                        List.of(
                                                                v.showId(),
                                                                v.movieId(),
                                                                v.movieTitle(),
                                                                v.hallId(),
                                                                v.hallName(),
                                                                v.showDate(),
                                                                v.showTime(),
                                                                v.showStatus(),
                                                                v.generatedSeatCount(),
                                                                v.ticketsSold(),
                                                                v.confirmedBookingCount(),
                                                                v.occupancyPercentage(),
                                                                m.currency(),
                                                                m.grossRevenue(),
                                                                m.refundAmount(),
                                                                m.netRevenue()))));
        return export(
                format,
                "show-performance-" + dates(range),
                List.of(
                        "showId",
                        "movieId",
                        "movieTitle",
                        "hallId",
                        "hallName",
                        "showDate",
                        "showTime",
                        "showStatus",
                        "generatedSeatCount",
                        "ticketsSold",
                        "confirmedBookingCount",
                        "occupancyPercentage",
                        "currency",
                        "grossRevenue",
                        "refundAmount",
                        "netRevenue"),
                rows);
    }

    private <T> List<T> pages(Function<Integer, PageResponse<T>> loader) {
        List<T> result = new ArrayList<>();
        for (int page = 0; ; page++) {
            PageResponse<T> response = loader.apply(page);
            if (response.getTotalElements() > properties.export().maxRows())
                throw new IllegalArgumentException("Report exceeds the maximum export row limit");
            result.addAll(response.getContent());
            if (response.isLast()) return result;
        }
    }

    private ReportExportResult export(
            ReportExportFormat format, String name, List<String> headers, List<List<Object>> rows) {
        if (rows.size() > properties.export().maxRows())
            throw new IllegalArgumentException("Report exceeds the maximum export row limit");
        ReportExportResult result =
                exporters.get(format).export(new ExportTable(headers, rows), name);
        if (result.content().length > properties.export().maxFileSizeBytes())
            throw new IllegalArgumentException("Generated report exceeds the maximum file size");
        metrics.counter("report_export_rows", "format", format.name()).increment(result.rowCount());
        metrics.counter("report_export_size_bytes", "format", format.name())
                .increment(result.content().length);
        return result;
    }

    private List<CurrencyPerformanceAmountResponse> currencies(
            List<CurrencyPerformanceAmountResponse> values) {
        return values.isEmpty()
                ? List.of(
                        new CurrencyPerformanceAmountResponse(
                                "",
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO,
                                java.math.BigDecimal.ZERO))
                : values;
    }

    private List<String> performanceHeaders(String id, String name, String status) {
        return List.of(
                id,
                name,
                status,
                "showCount",
                "confirmedBookingCount",
                "ticketsSold",
                "generatedSeatCount",
                "occupancyPercentage",
                "averageAttendancePerShow",
                "currency",
                "grossRevenue",
                "refundAmount",
                "netRevenue",
                "averageRevenuePerShow");
    }

    private String dates(ReportingDateRange range) {
        return range.startDate() + "-to-" + range.endDate();
    }
}
