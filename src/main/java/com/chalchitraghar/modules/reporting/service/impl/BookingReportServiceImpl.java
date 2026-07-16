package com.chalchitraghar.modules.reporting.service.impl;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.repository.ReportingAnalyticsRepository;
import com.chalchitraghar.modules.reporting.repository.ReportingQueryRepository;
import com.chalchitraghar.modules.reporting.service.*;
import java.time.LocalDate;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingReportServiceImpl implements BookingReportService {
    private final ReportingQueryRepository repository;
    private final ReportingRateCalculator rates;
    private final ReportingAnalyticsRepository analytics;
    private final ReportPeriodBuckets buckets;

    @Override
    public AdminBookingKpiResponse getKpis(ReportingDateRange range) {
        EnumMap<BookingStatus, Long> counts = new EnumMap<>(BookingStatus.class);
        Arrays.stream(BookingStatus.values()).forEach(status -> counts.put(status, 0L));
        repository
                .bookingStatusCounts(range.startInclusive(), range.endExclusive())
                .forEach(row -> counts.put(row.getStatus(), row.getCount()));
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long confirmed = counts.get(BookingStatus.CONFIRMED);
        long cancelled = counts.get(BookingStatus.CANCELLED);
        long expired = counts.get(BookingStatus.EXPIRED);
        List<BookingStatusCountResponse> breakdown =
                Arrays.stream(BookingStatus.values())
                        .map(status -> new BookingStatusCountResponse(status, counts.get(status)))
                        .toList();
        return new AdminBookingKpiResponse(
                ReportingPeriodResponse.from(range),
                total,
                confirmed,
                cancelled,
                expired,
                breakdown,
                rates.percentage(confirmed, total),
                rates.percentage(cancelled, total),
                rates.percentage(expired, total));
    }

    @Override
    public DetailedBookingReportResponse getDetailedReport(
            ReportingDateRange range, ReportGrouping grouping) {
        ReportGrouping effective = grouping == null ? ReportGrouping.DAY : grouping;
        Map<LocalDate, EnumMap<BookingStatus, Long>> grouped = new HashMap<>();
        analytics
                .dailyBookings(range.startInclusive(), range.endExclusive())
                .forEach(
                        row ->
                                grouped.computeIfAbsent(
                                                buckets.key(row.day(), effective), key -> empty())
                                        .merge(row.status(), row.count(), Long::sum));
        List<BookingTrendBucketResponse> trends =
                buckets.periods(range.startDate(), range.endDate(), effective).stream()
                        .map(period -> trend(period, grouped.getOrDefault(period.key(), empty())))
                        .toList();
        return new DetailedBookingReportResponse(getKpis(range), effective, trends);
    }

    private BookingTrendBucketResponse trend(
            ReportPeriodBuckets.Period period, EnumMap<BookingStatus, Long> counts) {
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        long confirmed = counts.get(BookingStatus.CONFIRMED);
        long cancelled = counts.get(BookingStatus.CANCELLED);
        long expired = counts.get(BookingStatus.EXPIRED);
        return new BookingTrendBucketResponse(
                period.start(),
                period.end(),
                total,
                Arrays.stream(BookingStatus.values())
                        .map(status -> new BookingStatusCountResponse(status, counts.get(status)))
                        .toList(),
                confirmed,
                cancelled,
                expired,
                rates.percentage(confirmed, total),
                rates.percentage(cancelled, total),
                rates.percentage(expired, total));
    }

    private EnumMap<BookingStatus, Long> empty() {
        EnumMap<BookingStatus, Long> values = new EnumMap<>(BookingStatus.class);
        Arrays.stream(BookingStatus.values()).forEach(status -> values.put(status, 0L));
        return values;
    }
}
