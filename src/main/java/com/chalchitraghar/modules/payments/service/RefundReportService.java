package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.config.RefundReportProperties;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.repository.RefundRepository;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundReportService {
    private final RefundRepository refunds;
    private final RefundReportProperties properties;

    @Transactional(readOnly = true)
    public AdminRefundSummaryReport summary(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null)
            throw new IllegalArgumentException("from and to are required");
        if (from.isAfter(to)) throw new IllegalArgumentException("from must not be after to");
        if (Duration.between(from, to).toDays() > properties.getMaxRangeDays())
            throw new IllegalArgumentException("Refund report range exceeds configured maximum");
        var items =
                refunds.aggregateStatus(from, to).stream()
                        .map(
                                x ->
                                        new RefundBreakdownItem(
                                                x.getStatus().name(),
                                                x.getCount(),
                                                x.getAmount(),
                                                x.getCurrency()))
                        .toList();
        long total = items.stream().mapToLong(RefundBreakdownItem::count).sum();
        long succeeded =
                items.stream()
                        .filter(x -> x.key().equals("SUCCEEDED"))
                        .mapToLong(RefundBreakdownItem::count)
                        .sum();
        long failed =
                items.stream()
                        .filter(x -> x.key().equals("FAILED"))
                        .mapToLong(RefundBreakdownItem::count)
                        .sum();
        return new AdminRefundSummaryReport(
                from,
                to,
                total,
                items,
                breakdown(refunds.aggregateReason(from, to)),
                breakdown(refunds.aggregateMethod(from, to)),
                breakdown(refunds.aggregateProvider(from, to)),
                refunds.countRetries(from, to),
                refunds.countExhausted(from, to),
                total == 0 ? 0d : (double) succeeded / total,
                total == 0 ? 0d : (double) failed / total,
                refunds.countDistinctSucceededBookings(from, to),
                refunds.countDistinctSucceededPayments(from, to));
    }

    private java.util.List<RefundBreakdownItem> breakdown(
            java.util.List<
                            com.chalchitraghar.modules.payments.repository
                                    .RefundNamedAggregateProjection>
                    values) {
        return values.stream()
                .map(
                        x ->
                                new RefundBreakdownItem(
                                        String.valueOf(x.getKey()),
                                        x.getCount(),
                                        x.getAmount(),
                                        x.getCurrency()))
                .toList();
    }
}
