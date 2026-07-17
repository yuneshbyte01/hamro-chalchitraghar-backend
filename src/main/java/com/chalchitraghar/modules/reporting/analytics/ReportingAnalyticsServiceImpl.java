package com.chalchitraghar.modules.reporting.analytics;

import com.chalchitraghar.modules.reporting.analytics.AnalyticsQueryRepository.*;
import com.chalchitraghar.modules.reporting.analytics.dto.*;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.service.*;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.function.IntFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ReportingAnalyticsServiceImpl implements ReportingAnalyticsService {
    private final AnalyticsQueryRepository queries;
    private final PerformanceReportService performance;
    private final RevenueReportService revenue;
    private final ReportingRateCalculator rates;

    public List<BookingPatternBucketResponse> bookingPatterns(
            ReportingDateRange range, BookingPatternGrouping grouping) {
        Map<String, PatternRow> values = new HashMap<>();
        for (PatternRow r : queries.bookingPatterns(range, grouping))
            values.put(patternKey(r.bucket(), grouping), r);
        List<String> labels =
                switch (grouping) {
                    case HOUR_OF_DAY ->
                            java.util.stream.IntStream.range(0, 24)
                                    .mapToObj(i -> String.format("%02d:00", i))
                                    .toList();
                    case DAY_OF_WEEK ->
                            List.of(
                                    "MONDAY",
                                    "TUESDAY",
                                    "WEDNESDAY",
                                    "THURSDAY",
                                    "FRIDAY",
                                    "SATURDAY",
                                    "SUNDAY");
                    case DATE ->
                            range.startDate()
                                    .datesUntil(range.endDate().plusDays(1))
                                    .map(LocalDate::toString)
                                    .toList();
                };
        long total = values.values().stream().mapToLong(PatternRow::total).sum();
        return labels.stream()
                .map(
                        label -> {
                            PatternRow r =
                                    values.getOrDefault(label, new PatternRow(label, 0, 0, 0));
                            return new BookingPatternBucketResponse(
                                    label,
                                    r.total(),
                                    r.confirmed(),
                                    r.cancelled(),
                                    rates.percentage(r.confirmed(), r.total()),
                                    rates.percentage(r.total(), total));
                        })
                .toList();
    }

    public CustomerAnalyticsResponse customers(ReportingDateRange range) {
        CustomerRow r = queries.customers(range);
        return new CustomerAnalyticsResponse(
                r.any(),
                r.confirmed(),
                r.firstTime(),
                r.repeat(),
                rates.percentage(r.repeat(), r.confirmed()),
                rates.average(BigDecimal.valueOf(r.confirmedBookings()), r.confirmed()),
                rates.average(BigDecimal.valueOf(r.confirmedSeats()), r.confirmed()),
                r.cancellations(),
                rates.percentage(r.cancellations(), r.any()));
    }

    public ConversionAnalyticsResponse conversion(ReportingDateRange range) {
        Map<String, Long> b = queries.bookingStatuses(range), p = queries.paymentStatuses(range);
        long total = sum(b),
                confirmed = get(b, "CONFIRMED"),
                cancelled = get(b, "CANCELLED"),
                expired = get(b, "EXPIRED"),
                open = get(b, "INITIATED") + get(b, "PENDING");
        long success = get(p, "SUCCESS") + get(p, "REFUNDED"),
                failed = get(p, "FAILED") + get(p, "CANCELLED"),
                paymentExpired = get(p, "EXPIRED"),
                terminal = success + failed + paymentExpired;
        return new ConversionAnalyticsResponse(
                total,
                confirmed,
                cancelled,
                expired,
                open,
                rates.percentage(confirmed, total),
                rates.percentage(cancelled, total),
                rates.percentage(expired, total),
                terminal,
                success,
                failed,
                paymentExpired,
                rates.percentage(success, terminal),
                rates.percentage(failed, terminal),
                rates.percentage(paymentExpired, terminal),
                b);
    }

    public RefundAnalyticsResponse refunds(ReportingDateRange range, String currency) {
        String normalized = rates.currency(currency);
        RefundSummaryRow s = queries.refundSummary(range);
        long paid = queries.recognizedPaidBookings(range);
        var currencies =
                queries.refundCurrencies(range, normalized).stream()
                        .map(
                                r ->
                                        new CurrencyRefundAnalyticsResponse(
                                                r.currency(),
                                                scale(r.refunded()),
                                                scale(r.gross()),
                                                percent(r.refunded(), r.gross()),
                                                rates.average(r.refunded(), r.refundCount())))
                        .toList();
        return new RefundAnalyticsResponse(
                s.succeeded(),
                s.failed(),
                s.pending(),
                s.refundedBookings(),
                paid,
                rates.percentage(s.refundedBookings(), paid),
                queries.topRefundReason(range),
                queries.refundBreakdown(range, "refund_method"),
                queries.refundBreakdown(range, "provider"),
                currencies);
    }

    public List<SeatUtilizationRowResponse> seats(
            ReportingDateRange range, Long movieId, Long hallId, String seatType) {
        List<SeatUtilizationRowResponse> result = new ArrayList<>();
        for (SeatDimension d : SeatDimension.values()) {
            Map<String, List<SeatMoneyRow>> money = new HashMap<>();
            queries.seatMoney(range, movieId, hallId, seatType, d)
                    .forEach(
                            r ->
                                    money.computeIfAbsent(r.dimension(), k -> new ArrayList<>())
                                            .add(r));
            for (SeatRow r : queries.seats(range, movieId, hallId, seatType, d)) {
                Map<String, BigDecimal> value = new TreeMap<>(), average = new TreeMap<>();
                for (SeatMoneyRow m : money.getOrDefault(r.dimension(), List.of())) {
                    value.put(m.currency(), scale(m.value()));
                    average.put(m.currency(), rates.average(m.value(), m.sold()));
                }
                result.add(
                        new SeatUtilizationRowResponse(
                                d.name(),
                                r.dimension(),
                                r.generated(),
                                r.sold(),
                                Math.max(0, r.generated() - r.sold()),
                                rates.percentage(r.sold(), r.generated()),
                                value,
                                average));
            }
        }
        return result;
    }

    public List<ShowTimeAnalyticsResponse> showTimes(
            ReportingDateRange range, String currency, ShowTimeGrouping grouping) {
        Map<String, ShowTimeAccumulator> map = new TreeMap<>();
        for (ShowPerformanceResponse show :
                all(
                        page ->
                                performance.shows(
                                        range,
                                        null,
                                        null,
                                        null,
                                        currency,
                                        ShowPerformanceSort.SHOW_DATE,
                                        ReportSortDirection.ASC,
                                        page,
                                        100))) {
            String key =
                    grouping == ShowTimeGrouping.HOUR
                            ? String.format("%02d:00", show.showTime().getHour())
                            : slot(show.showTime());
            map.computeIfAbsent(key, k -> new ShowTimeAccumulator()).add(show);
        }
        List<String> labels =
                grouping == ShowTimeGrouping.HOUR
                        ? java.util.stream.IntStream.range(0, 24)
                                .mapToObj(i -> String.format("%02d:00", i))
                                .toList()
                        : List.of("MORNING", "AFTERNOON", "EVENING", "NIGHT");
        return labels.stream()
                .map(k -> map.getOrDefault(k, new ShowTimeAccumulator()).response(k, rates))
                .toList();
    }

    public RevenueConcentrationResponse concentration(
            ReportingDateRange range, String currency, ConcentrationDimension dimension, int top) {
        String normalized = rates.currency(currency);
        if (normalized == null) throw new IllegalArgumentException("currency is required");
        if (top < 1 || top > 20) throw new IllegalArgumentException("top must be between 1 and 20");
        List<EntityRevenue> entities =
                switch (dimension) {
                    case MOVIE ->
                            all(
                                            page ->
                                                    performance.movies(
                                                            range,
                                                            normalized,
                                                            MoviePerformanceSort.TITLE,
                                                            ReportSortDirection.ASC,
                                                            page,
                                                            100))
                                    .stream()
                                    .map(
                                            v ->
                                                    new EntityRevenue(
                                                            v.movieId(),
                                                            v.movieTitle(),
                                                            gross(
                                                                    v.revenueByCurrency(),
                                                                    normalized)))
                                    .toList();
                    case HALL ->
                            all(
                                            page ->
                                                    performance.halls(
                                                            range,
                                                            normalized,
                                                            HallPerformanceSort.HALL_NAME,
                                                            ReportSortDirection.ASC,
                                                            page,
                                                            100))
                                    .stream()
                                    .map(
                                            v ->
                                                    new EntityRevenue(
                                                            v.hallId(),
                                                            v.hallName(),
                                                            gross(
                                                                    v.revenueByCurrency(),
                                                                    normalized)))
                                    .toList();
                    case SHOW ->
                            all(
                                            page ->
                                                    performance.shows(
                                                            range,
                                                            null,
                                                            null,
                                                            null,
                                                            normalized,
                                                            ShowPerformanceSort.SHOW_DATE,
                                                            ReportSortDirection.ASC,
                                                            page,
                                                            100))
                                    .stream()
                                    .map(
                                            v ->
                                                    new EntityRevenue(
                                                            v.showId(),
                                                            v.movieTitle()
                                                                    + " — "
                                                                    + v.showDate()
                                                                    + " "
                                                                    + v.showTime(),
                                                            gross(
                                                                    v.revenueByCurrency(),
                                                                    normalized)))
                                    .toList();
                };
        entities = new ArrayList<>(entities);
        entities.sort(
                Comparator.comparing(EntityRevenue::revenue)
                        .reversed()
                        .thenComparing(EntityRevenue::name)
                        .thenComparingLong(EntityRevenue::id));
        BigDecimal
                total =
                        entities.stream()
                                .map(EntityRevenue::revenue)
                                .reduce(BigDecimal.ZERO, BigDecimal::add),
                cumulative = BigDecimal.ZERO;
        List<ConcentrationEntityResponse> rows = new ArrayList<>();
        for (EntityRevenue e : entities.stream().limit(top).toList()) {
            cumulative = cumulative.add(e.revenue());
            rows.add(
                    new ConcentrationEntityResponse(
                            e.id(),
                            e.name(),
                            scale(e.revenue()),
                            percent(e.revenue(), total),
                            percent(cumulative, total)));
        }
        BigDecimal used =
                rows.stream()
                        .map(ConcentrationEntityResponse::revenue)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new RevenueConcentrationResponse(
                normalized,
                dimension,
                scale(total),
                rows,
                scale(total.subtract(used)),
                percent(total.subtract(used), total));
    }

    public PerformanceExtremesResponse extremes(
            ReportingDateRange range, String currency, int minimumShows) {
        if (minimumShows < 1) throw new IllegalArgumentException("minimumShows must be at least 1");
        String normalized = rates.currency(currency);
        Map<String, PerformanceExtremeResponse> best = new LinkedHashMap<>(),
                low = new LinkedHashMap<>();
        var movies =
                all(
                                p ->
                                        performance.movies(
                                                range,
                                                normalized,
                                                MoviePerformanceSort.TITLE,
                                                ReportSortDirection.ASC,
                                                p,
                                                100))
                        .stream()
                        .filter(v -> v.showCount() >= minimumShows)
                        .toList();
        var halls =
                all(
                                p ->
                                        performance.halls(
                                                range,
                                                normalized,
                                                HallPerformanceSort.HALL_NAME,
                                                ReportSortDirection.ASC,
                                                p,
                                                100))
                        .stream()
                        .filter(v -> v.showCount() >= minimumShows)
                        .toList();
        var shows =
                all(
                        p ->
                                performance.shows(
                                        range,
                                        null,
                                        null,
                                        null,
                                        normalized,
                                        ShowPerformanceSort.SHOW_DATE,
                                        ReportSortDirection.ASC,
                                        p,
                                        100));
        putExtremes(
                "movieOccupancy",
                movies,
                v -> v.occupancyPercentage(),
                v ->
                        new PerformanceExtremeResponse(
                                v.movieId(),
                                v.movieTitle(),
                                v.occupancyPercentage(),
                                v.showCount()),
                best,
                low);
        putExtremes(
                "movieTickets",
                movies,
                v -> BigDecimal.valueOf(v.ticketsSold()),
                v ->
                        new PerformanceExtremeResponse(
                                v.movieId(),
                                v.movieTitle(),
                                BigDecimal.valueOf(v.ticketsSold()),
                                v.showCount()),
                best,
                low);
        putExtremes(
                "hallOccupancy",
                halls,
                v -> v.occupancyPercentage(),
                v ->
                        new PerformanceExtremeResponse(
                                v.hallId(), v.hallName(), v.occupancyPercentage(), v.showCount()),
                best,
                low);
        putExtremes(
                "hallTickets",
                halls,
                v -> BigDecimal.valueOf(v.ticketsSold()),
                v ->
                        new PerformanceExtremeResponse(
                                v.hallId(),
                                v.hallName(),
                                BigDecimal.valueOf(v.ticketsSold()),
                                v.showCount()),
                best,
                low);
        putExtremes(
                "showOccupancy",
                shows,
                v -> v.occupancyPercentage(),
                v ->
                        new PerformanceExtremeResponse(
                                v.showId(), v.movieTitle(), v.occupancyPercentage(), 1),
                best,
                low);
        putExtremes(
                "showTickets",
                shows,
                v -> BigDecimal.valueOf(v.ticketsSold()),
                v ->
                        new PerformanceExtremeResponse(
                                v.showId(), v.movieTitle(), BigDecimal.valueOf(v.ticketsSold()), 1),
                best,
                low);
        if (normalized != null) {
            putExtremes(
                    "movieRevenue",
                    movies,
                    v -> gross(v.revenueByCurrency(), normalized),
                    v ->
                            new PerformanceExtremeResponse(
                                    v.movieId(),
                                    v.movieTitle(),
                                    gross(v.revenueByCurrency(), normalized),
                                    v.showCount()),
                    best,
                    low);
            putExtremes(
                    "hallRevenue",
                    halls,
                    v -> gross(v.revenueByCurrency(), normalized),
                    v ->
                            new PerformanceExtremeResponse(
                                    v.hallId(),
                                    v.hallName(),
                                    gross(v.revenueByCurrency(), normalized),
                                    v.showCount()),
                    best,
                    low);
            putExtremes(
                    "showRevenue",
                    shows,
                    v -> gross(v.revenueByCurrency(), normalized),
                    v ->
                            new PerformanceExtremeResponse(
                                    v.showId(),
                                    v.movieTitle(),
                                    gross(v.revenueByCurrency(), normalized),
                                    1),
                    best,
                    low);
        }
        return new PerformanceExtremesResponse(normalized, minimumShows, best, low);
    }

    public DataQualityAnalyticsResponse dataQuality() {
        DataQualityRow r = queries.dataQuality();
        long total = r.missingPayment() + r.paymentState() + r.failedDelivery();
        return new DataQualityAnalyticsResponse(
                r.missingPayment(),
                r.paymentState(),
                r.duplicatePayment(),
                r.failedDelivery(),
                total);
    }

    public AnalyticsOverviewResponse overview(ReportingDateRange range, String currency) {
        var customer = customers(range);
        var conversion = conversion(range);
        var refund = refunds(range, currency);
        var patterns = bookingPatterns(range, BookingPatternGrouping.HOUR_OF_DAY);
        var weekdays = bookingPatterns(range, BookingPatternGrouping.DAY_OF_WEEK);
        var times = showTimes(range, currency, ShowTimeGrouping.TIME_SLOT);
        var extremes = extremes(range, currency, 1);
        BigDecimal refundAmountRate =
                refund.currencies().size() == 1
                        ? refund.currencies().get(0).refundAmountRate()
                        : null;
        BigDecimal concentration =
                currency == null
                        ? null
                        : concentration(range, currency, ConcentrationDimension.MOVIE, 5)
                                .topEntities()
                                .stream()
                                .map(ConcentrationEntityResponse::percentageOfTotal)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long seats = queries.customers(range).confirmedSeats();
        BigDecimal avgSeats =
                rates.average(BigDecimal.valueOf(seats), conversion.confirmedBookings());
        BigDecimal avgPayment =
                revenue.getKpis(range, currency).currencies().size() == 1
                        ? revenue.getKpis(range, currency)
                                .currencies()
                                .get(0)
                                .averageSuccessfulPaymentAmount()
                        : null;
        return new AnalyticsOverviewResponse(
                customer.repeatCustomerRate(),
                conversion.bookingAttemptConversionRate(),
                conversion.cancellationRate(),
                refund.refundCountRate(),
                refundAmountRate,
                avgSeats,
                avgPayment,
                concentration,
                peak(patterns),
                peak(weekdays),
                extremes.best().get("movieOccupancy"),
                extremes.best().get("hallOccupancy"),
                peakShowTime(times),
                extremes.lowest().get("movieOccupancy"),
                extremes.lowest().get("hallOccupancy"),
                dataQuality().totalWarnings());
    }

    private String patternKey(String raw, BookingPatternGrouping grouping) {
        if (grouping == BookingPatternGrouping.HOUR_OF_DAY)
            return String.format("%02d:00", new BigDecimal(raw).intValue());
        if (grouping == BookingPatternGrouping.DATE) return raw;
        int value = new BigDecimal(raw).intValue();
        return DayOfWeek.of(value == 0 ? 7 : value).name();
    }

    private String slot(LocalTime time) {
        int h = time.getHour();
        if (h >= 5 && h < 12) return "MORNING";
        if (h >= 12 && h < 17) return "AFTERNOON";
        if (h >= 17 && h < 21) return "EVENING";
        return "NIGHT";
    }

    private String peak(List<BookingPatternBucketResponse> rows) {
        return rows.stream()
                .max(
                        Comparator.comparingLong(BookingPatternBucketResponse::bookingCount)
                                .thenComparing(
                                        BookingPatternBucketResponse::label,
                                        Comparator.reverseOrder()))
                .filter(v -> v.bookingCount() > 0)
                .map(BookingPatternBucketResponse::label)
                .orElse(null);
    }

    private String peakShowTime(List<ShowTimeAnalyticsResponse> rows) {
        return rows.stream()
                .max(
                        Comparator.comparing(ShowTimeAnalyticsResponse::occupancyPercentage)
                                .thenComparing(
                                        ShowTimeAnalyticsResponse::bucket,
                                        Comparator.reverseOrder()))
                .filter(v -> v.eligibleShowCount() > 0)
                .map(ShowTimeAnalyticsResponse::bucket)
                .orElse(null);
    }

    private long sum(Map<String, Long> values) {
        return values.values().stream().mapToLong(Long::longValue).sum();
    }

    private long get(Map<String, Long> values, String key) {
        return values.getOrDefault(key, 0L);
    }

    private BigDecimal gross(List<CurrencyPerformanceAmountResponse> values, String currency) {
        return values.stream()
                .filter(v -> v.currency().equals(currency))
                .map(CurrencyPerformanceAmountResponse::grossRevenue)
                .findFirst()
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal scale(BigDecimal value) {
        return value == null
                ? BigDecimal.ZERO.setScale(2)
                : value.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal percent(BigDecimal n, BigDecimal d) {
        if (d == null || d.signum() == 0) return BigDecimal.ZERO.setScale(2);
        return n.multiply(BigDecimal.valueOf(100)).divide(d, 2, RoundingMode.HALF_UP);
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

    private <T> void putExtremes(
            String key,
            List<T> values,
            java.util.function.Function<T, BigDecimal> metric,
            java.util.function.Function<T, PerformanceExtremeResponse> mapper,
            Map<String, PerformanceExtremeResponse> best,
            Map<String, PerformanceExtremeResponse> low) {
        values.stream()
                .max(Comparator.comparing(metric))
                .map(mapper)
                .ifPresent(v -> best.put(key, v));
        values.stream()
                .min(Comparator.comparing(metric))
                .map(mapper)
                .ifPresent(v -> low.put(key, v));
    }

    private record EntityRevenue(long id, String name, BigDecimal revenue) {}

    private static class ShowTimeAccumulator {
        long shows, tickets, seats, bookings;
        Map<String, Money> money = new TreeMap<>();

        void add(ShowPerformanceResponse v) {
            shows++;
            tickets += v.ticketsSold();
            seats += v.generatedSeatCount();
            bookings += v.confirmedBookingCount();
            v.revenueByCurrency()
                    .forEach(r -> money.computeIfAbsent(r.currency(), k -> new Money()).add(r));
        }

        ShowTimeAnalyticsResponse response(String key, ReportingRateCalculator rates) {
            List<CurrencyPerformanceAmountResponse> currencies =
                    money.entrySet().stream()
                            .map(
                                    e ->
                                            new CurrencyPerformanceAmountResponse(
                                                    e.getKey(),
                                                    e.getValue().gross,
                                                    e.getValue().refund,
                                                    e.getValue()
                                                            .gross
                                                            .subtract(e.getValue().refund),
                                                    rates.average(e.getValue().gross, shows)))
                            .toList();
            return new ShowTimeAnalyticsResponse(
                    key,
                    shows,
                    tickets,
                    seats,
                    rates.percentage(tickets, seats),
                    bookings,
                    rates.average(BigDecimal.valueOf(tickets), shows),
                    currencies);
        }
    }

    private static class Money {
        BigDecimal gross = BigDecimal.ZERO, refund = BigDecimal.ZERO;

        void add(CurrencyPerformanceAmountResponse r) {
            gross = gross.add(r.grossRevenue());
            refund = refund.add(r.refundAmount());
        }
    }
}
