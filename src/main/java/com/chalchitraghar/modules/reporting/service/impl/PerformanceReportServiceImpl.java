package com.chalchitraghar.modules.reporting.service.impl;

import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.repository.ReportingAnalyticsRepository;
import com.chalchitraghar.modules.reporting.repository.ReportingAnalyticsRepository.Dimension;
import com.chalchitraghar.modules.reporting.service.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.shared.response.PageResponse;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceReportServiceImpl implements PerformanceReportService {
    private final ReportingAnalyticsRepository repository;
    private final ReportingRateCalculator rates;

    @Override
    public PageResponse<MoviePerformanceResponse> movies(
            ReportingDateRange range,
            String currency,
            MoviePerformanceSort sort,
            ReportSortDirection direction,
            int page,
            int size) {
        String normalized = rates.currency(currency);
        var rows =
                page(
                        Dimension.MOVIE,
                        range,
                        null,
                        null,
                        null,
                        normalized,
                        (sort == null ? MoviePerformanceSort.REVENUE : sort).name(),
                        direction,
                        page,
                        size);
        Map<Long, List<CurrencyPerformanceAmountResponse>> money =
                money(Dimension.MOVIE, rows.content(), range, normalized);
        List<MoviePerformanceResponse> content =
                rows.content().stream()
                        .map(
                                row ->
                                        new MoviePerformanceResponse(
                                                row.id(),
                                                row.name(),
                                                MovieStatus.valueOf(row.status()),
                                                row.showCount(),
                                                row.bookingCount(),
                                                row.sold(),
                                                row.capacity(),
                                                rates.percentage(row.sold(), row.capacity()),
                                                money.getOrDefault(row.id(), List.of()),
                                                rates.average(
                                                        BigDecimal.valueOf(row.sold()),
                                                        row.showCount())))
                        .toList();
        return response(content, page, size, rows.total());
    }

    @Override
    public PageResponse<HallPerformanceResponse> halls(
            ReportingDateRange range,
            String currency,
            HallPerformanceSort sort,
            ReportSortDirection direction,
            int page,
            int size) {
        String normalized = rates.currency(currency);
        var rows =
                page(
                        Dimension.HALL,
                        range,
                        null,
                        null,
                        null,
                        normalized,
                        (sort == null ? HallPerformanceSort.REVENUE : sort).name(),
                        direction,
                        page,
                        size);
        Map<Long, List<CurrencyPerformanceAmountResponse>> money =
                money(Dimension.HALL, rows.content(), range, normalized);
        List<HallPerformanceResponse> content =
                rows.content().stream()
                        .map(
                                row ->
                                        new HallPerformanceResponse(
                                                row.id(),
                                                row.name(),
                                                Status.valueOf(row.status()),
                                                row.configuredCapacity(),
                                                row.showCount(),
                                                row.bookingCount(),
                                                row.sold(),
                                                row.capacity(),
                                                rates.percentage(row.sold(), row.capacity()),
                                                money.getOrDefault(row.id(), List.of()),
                                                rates.average(
                                                        BigDecimal.valueOf(row.sold()),
                                                        row.showCount())))
                        .toList();
        return response(content, page, size, rows.total());
    }

    @Override
    public PageResponse<ShowPerformanceResponse> shows(
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            String currency,
            ShowPerformanceSort sort,
            ReportSortDirection direction,
            int page,
            int size) {
        String normalized = rates.currency(currency);
        var rows =
                page(
                        Dimension.SHOW,
                        range,
                        movieId,
                        hallId,
                        status,
                        normalized,
                        (sort == null ? ShowPerformanceSort.SHOW_DATE : sort).name(),
                        direction,
                        page,
                        size);
        Map<Long, List<CurrencyPerformanceAmountResponse>> money =
                money(Dimension.SHOW, rows.content(), range, normalized);
        List<ShowPerformanceResponse> content =
                rows.content().stream()
                        .map(
                                row ->
                                        new ShowPerformanceResponse(
                                                row.id(),
                                                row.movieId(),
                                                row.movieTitle(),
                                                row.hallId(),
                                                row.hallName(),
                                                row.showDate(),
                                                row.showTime(),
                                                ShowStatus.valueOf(row.status()),
                                                row.capacity(),
                                                row.sold(),
                                                row.bookingCount(),
                                                rates.percentage(row.sold(), row.capacity()),
                                                money.getOrDefault(row.id(), List.of())))
                        .toList();
        return response(content, page, size, rows.total());
    }

    private ReportingAnalyticsRepository.PageRows<ReportingAnalyticsRepository.DimensionRow> page(
            Dimension dimension,
            ReportingDateRange range,
            Long movieId,
            Long hallId,
            ShowStatus status,
            String currency,
            String sort,
            ReportSortDirection direction,
            int page,
            int size) {
        validatePage(page, size);
        ReportSortDirection effective = direction == null ? ReportSortDirection.DESC : direction;
        return repository.dimensionPage(
                dimension,
                range.startDate(),
                range.endDate(),
                movieId,
                hallId,
                status,
                currency,
                sort,
                effective,
                page,
                size);
    }

    private Map<Long, List<CurrencyPerformanceAmountResponse>> money(
            Dimension dimension,
            List<ReportingAnalyticsRepository.DimensionRow> rows,
            ReportingDateRange range,
            String currency) {
        List<Long> ids = rows.stream().map(ReportingAnalyticsRepository.DimensionRow::id).toList();
        Map<Long, Map<String, Amounts>> values = new HashMap<>();
        repository
                .dimensionMoney(dimension, ids, range.startDate(), range.endDate(), currency, false)
                .forEach(
                        row ->
                                values.computeIfAbsent(row.id(), k -> new TreeMap<>())
                                                .computeIfAbsent(row.currency(), k -> new Amounts())
                                                .gross =
                                        row.amount());
        repository
                .dimensionMoney(dimension, ids, range.startDate(), range.endDate(), currency, true)
                .forEach(
                        row ->
                                values.computeIfAbsent(row.id(), k -> new TreeMap<>())
                                                .computeIfAbsent(row.currency(), k -> new Amounts())
                                                .refund =
                                        row.amount());
        if (currency != null)
            ids.forEach(
                    id ->
                            values.computeIfAbsent(id, k -> new TreeMap<>())
                                    .computeIfAbsent(currency, k -> new Amounts()));
        Map<Long, List<CurrencyPerformanceAmountResponse>> result = new HashMap<>();
        rows.forEach(
                row ->
                        result.put(
                                row.id(),
                                values.getOrDefault(row.id(), Map.of()).entrySet().stream()
                                        .map(
                                                e ->
                                                        e.getValue()
                                                                .response(
                                                                        e.getKey(),
                                                                        row.showCount(),
                                                                        rates))
                                        .toList()));
        return result;
    }

    private void validatePage(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException(
                    "page must be non-negative and size must be between 1 and 100");
    }

    private <T> PageResponse<T> response(List<T> content, int page, int size, long total) {
        int pages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(content, page, size, total, pages, page + 1 >= pages);
    }

    private static final class Amounts {
        BigDecimal gross = BigDecimal.ZERO;
        BigDecimal refund = BigDecimal.ZERO;

        CurrencyPerformanceAmountResponse response(
                String currency, long shows, ReportingRateCalculator rates) {
            return new CurrencyPerformanceAmountResponse(
                    currency,
                    gross.setScale(2),
                    refund.setScale(2),
                    gross.subtract(refund).setScale(2),
                    rates.average(gross, shows));
        }
    }
}
