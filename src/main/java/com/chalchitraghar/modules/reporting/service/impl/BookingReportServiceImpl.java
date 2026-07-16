package com.chalchitraghar.modules.reporting.service.impl;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.repository.ReportingQueryRepository;
import com.chalchitraghar.modules.reporting.service.*;
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
}
