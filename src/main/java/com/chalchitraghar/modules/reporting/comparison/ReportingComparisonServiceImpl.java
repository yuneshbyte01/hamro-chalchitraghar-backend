package com.chalchitraghar.modules.reporting.comparison;

import com.chalchitraghar.modules.halls.repository.HallRepository;
import com.chalchitraghar.modules.movies.repository.MovieRepository;
import com.chalchitraghar.modules.reporting.comparison.dto.*;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.enums.ComparisonMode;
import com.chalchitraghar.modules.reporting.repository.ReportingQueryRepository;
import com.chalchitraghar.modules.reporting.service.*;
import com.chalchitraghar.modules.users.enums.Role;
import java.math.*;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportingComparisonServiceImpl implements ReportingComparisonService {
    private final RevenueReportService revenue;
    private final BookingReportService bookings;
    private final PerformanceReportService performance;
    private final ReportingQueryRepository queries;
    private final MovieRepository movies;
    private final HallRepository halls;
    private final Clock clock;

    public PeriodComparisonResponse periods(
            ReportingDateRange current,
            ComparisonMode mode,
            LocalDate previousStart,
            LocalDate previousEnd,
            String currency) {
        ReportingDateRange previous;
        if (mode == ComparisonMode.CUSTOM) {
            if (previousStart == null || previousEnd == null)
                throw new IllegalArgumentException(
                        "Custom comparison requires previousStartDate and previousEndDate");
            previous = ReportingDateRange.of(previousStart, previousEnd, clock);
        } else {
            long days = ChronoUnit.DAYS.between(current.startDate(), current.endDate()) + 1;
            LocalDate end = current.startDate().minusDays(1);
            previous = ReportingDateRange.of(end.minusDays(days - 1), end, clock);
        }
        var currentRevenue = revenue.getKpis(current, currency);
        var previousRevenue = revenue.getKpis(previous, currency);
        Map<String, CurrencyRevenueKpiResponse> a = index(currentRevenue.currencies());
        Map<String, CurrencyRevenueKpiResponse> b = index(previousRevenue.currencies());
        Set<String> currencies = new TreeSet<>(a.keySet());
        currencies.addAll(b.keySet());
        var money =
                currencies.stream().map(code -> currency(code, a.get(code), b.get(code))).toList();
        var cb = bookings.getKpis(current);
        var pb = bookings.getKpis(previous);
        return new PeriodComparisonResponse(
                currentRevenue.period(),
                previousRevenue.period(),
                money,
                metric(cb.totalBookings(), pb.totalBookings()),
                metric(cb.confirmedBookings(), pb.confirmedBookings()),
                metric(cb.cancelledBookings(), pb.cancelledBookings()),
                metric(cb.expiredBookings(), pb.expiredBookings()),
                metric(
                        queries.registeredUsers(
                                Role.CUSTOMER, current.startInclusive(), current.endExclusive()),
                        queries.registeredUsers(
                                Role.CUSTOMER, previous.startInclusive(), previous.endExclusive())),
                metric(cb.confirmationRate(), pb.confirmationRate()),
                metric(cb.cancellationRate(), pb.cancellationRate()));
    }

    public List<MovieComparisonResponse> movies(
            ReportingDateRange range, List<Long> ids, String currency) {
        validate(ids, movies::existsById, "movie");
        var values =
                all(
                                page ->
                                        performance.movies(
                                                range,
                                                currency,
                                                MoviePerformanceSort.TITLE,
                                                ReportSortDirection.ASC,
                                                page,
                                                100))
                        .stream()
                        .filter(v -> ids.contains(v.movieId()))
                        .toList();
        return rankMovies(values, currency);
    }

    public List<HallComparisonResponse> halls(
            ReportingDateRange range, List<Long> ids, String currency) {
        validate(ids, halls::existsById, "hall");
        var values =
                all(
                                page ->
                                        performance.halls(
                                                range,
                                                currency,
                                                HallPerformanceSort.HALL_NAME,
                                                ReportSortDirection.ASC,
                                                page,
                                                100))
                        .stream()
                        .filter(v -> ids.contains(v.hallId()))
                        .toList();
        return rankHalls(values, currency);
    }

    private CurrencyPeriodComparisonResponse currency(
            String code, CurrencyRevenueKpiResponse current, CurrencyRevenueKpiResponse previous) {
        var zero =
                new CurrencyRevenueKpiResponse(
                        code,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        0,
                        0,
                        BigDecimal.ZERO);
        current = current == null ? zero : current;
        previous = previous == null ? zero : previous;
        return new CurrencyPeriodComparisonResponse(
                code,
                metric(current.grossRevenue(), previous.grossRevenue()),
                metric(current.refundAmount(), previous.refundAmount()),
                metric(current.netRevenue(), previous.netRevenue()),
                metric(current.successfulPaymentCount(), previous.successfulPaymentCount()),
                metric(current.successfulRefundCount(), previous.successfulRefundCount()));
    }

    private MetricComparisonResponse metric(long current, long previous) {
        return metric(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }

    private MetricComparisonResponse metric(BigDecimal current, BigDecimal previous) {
        BigDecimal change = current.subtract(previous).setScale(2, RoundingMode.HALF_UP);
        boolean comparable = previous.signum() != 0 || current.signum() == 0;
        BigDecimal percentage =
                previous.signum() == 0
                        ? (current.signum() == 0 ? BigDecimal.ZERO.setScale(2) : null)
                        : change.multiply(BigDecimal.valueOf(100))
                                .divide(previous, 2, RoundingMode.HALF_UP);
        return new MetricComparisonResponse(current, previous, change, percentage, comparable);
    }

    private Map<String, CurrencyRevenueKpiResponse> index(List<CurrencyRevenueKpiResponse> values) {
        Map<String, CurrencyRevenueKpiResponse> result = new HashMap<>();
        values.forEach(v -> result.put(v.currency(), v));
        return result;
    }

    private void validate(List<Long> ids, Predicate<Long> exists, String label) {
        if (ids == null
                || ids.size() < 2
                || ids.size() > 5
                || new HashSet<>(ids).size() != ids.size())
            throw new IllegalArgumentException("Require 2 to 5 unique " + label + " IDs");
        ids.forEach(
                id -> {
                    if (!exists.test(id))
                        throw new IllegalArgumentException("Unknown " + label + " ID: " + id);
                });
    }

    private <T> List<T> all(
            IntFunction<com.chalchitraghar.shared.response.PageResponse<T>> loader) {
        List<T> result = new ArrayList<>();
        for (int page = 0; ; page++) {
            var p = loader.apply(page);
            result.addAll(p.getContent());
            if (p.isLast()) return result;
        }
    }

    private BigDecimal gross(List<CurrencyPerformanceAmountResponse> values, String currency) {
        return values.stream()
                .filter(
                        v ->
                                currency == null
                                        || v.currency()
                                                .equals(currency.trim().toUpperCase(Locale.ROOT)))
                .map(CurrencyPerformanceAmountResponse::grossRevenue)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private List<MovieComparisonResponse> rankMovies(
            List<MoviePerformanceResponse> values, String currency) {
        var ticket = new ArrayList<>(values);
        ticket.sort(
                Comparator.comparingLong(MoviePerformanceResponse::ticketsSold)
                        .reversed()
                        .thenComparing(MoviePerformanceResponse::movieTitle)
                        .thenComparingLong(MoviePerformanceResponse::movieId));
        var occupancy = new ArrayList<>(values);
        occupancy.sort(
                Comparator.comparing(MoviePerformanceResponse::occupancyPercentage)
                        .reversed()
                        .thenComparing(MoviePerformanceResponse::movieTitle)
                        .thenComparingLong(MoviePerformanceResponse::movieId));
        var revenue = new ArrayList<>(values);
        if (currency != null)
            revenue.sort(
                    Comparator.comparing(
                                    (MoviePerformanceResponse v) ->
                                            gross(v.revenueByCurrency(), currency))
                            .reversed()
                            .thenComparing(MoviePerformanceResponse::movieTitle)
                            .thenComparingLong(MoviePerformanceResponse::movieId));
        return values.stream()
                .map(
                        v ->
                                new MovieComparisonResponse(
                                        v,
                                        currency == null ? null : revenue.indexOf(v) + 1,
                                        ticket.indexOf(v) + 1,
                                        occupancy.indexOf(v) + 1))
                .toList();
    }

    private List<HallComparisonResponse> rankHalls(
            List<HallPerformanceResponse> values, String currency) {
        var ticket = new ArrayList<>(values);
        ticket.sort(
                Comparator.comparingLong(HallPerformanceResponse::ticketsSold)
                        .reversed()
                        .thenComparing(HallPerformanceResponse::hallName)
                        .thenComparingLong(HallPerformanceResponse::hallId));
        var occupancy = new ArrayList<>(values);
        occupancy.sort(
                Comparator.comparing(HallPerformanceResponse::occupancyPercentage)
                        .reversed()
                        .thenComparing(HallPerformanceResponse::hallName)
                        .thenComparingLong(HallPerformanceResponse::hallId));
        var revenue = new ArrayList<>(values);
        if (currency != null)
            revenue.sort(
                    Comparator.comparing(
                                    (HallPerformanceResponse v) ->
                                            gross(v.revenueByCurrency(), currency))
                            .reversed()
                            .thenComparing(HallPerformanceResponse::hallName)
                            .thenComparingLong(HallPerformanceResponse::hallId));
        return values.stream()
                .map(
                        v ->
                                new HallComparisonResponse(
                                        v,
                                        currency == null ? null : revenue.indexOf(v) + 1,
                                        ticket.indexOf(v) + 1,
                                        occupancy.indexOf(v) + 1))
                .toList();
    }
}
