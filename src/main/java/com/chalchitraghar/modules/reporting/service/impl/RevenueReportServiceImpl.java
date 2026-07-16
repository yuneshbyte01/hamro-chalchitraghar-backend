package com.chalchitraghar.modules.reporting.service.impl;

import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.reporting.dto.request.*;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.projection.*;
import com.chalchitraghar.modules.reporting.repository.ReportingAnalyticsRepository;
import com.chalchitraghar.modules.reporting.repository.ReportingQueryRepository;
import com.chalchitraghar.modules.reporting.service.*;
import java.math.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevenueReportServiceImpl implements RevenueReportService {
    private static final Set<PaymentStatus> RECOGNIZED =
            Set.of(PaymentStatus.SUCCESS, PaymentStatus.REFUNDED);
    private final ReportingQueryRepository repository;
    private final ReportingAnalyticsRepository analytics;
    private final ReportingRateCalculator calculator;
    private final ReportPeriodBuckets buckets;

    @Override
    public AdminRevenueKpiResponse getKpis(ReportingDateRange range, String currency) {
        String normalized = calculator.currency(currency);
        Map<String, RevenueValues> values = new TreeMap<>();
        repository
                .paymentRevenue(
                        RECOGNIZED, range.startInclusive(), range.endExclusive(), normalized)
                .forEach(
                        row ->
                                values.computeIfAbsent(
                                                row.getCurrency(), key -> new RevenueValues())
                                        .payment(row));
        repository
                .refundRevenue(
                        RefundStatus.SUCCEEDED,
                        range.startInclusive(),
                        range.endExclusive(),
                        normalized)
                .forEach(
                        row ->
                                values.computeIfAbsent(
                                                row.getCurrency(), key -> new RevenueValues())
                                        .refund(row));
        if (normalized != null) values.computeIfAbsent(normalized, key -> new RevenueValues());
        List<CurrencyRevenueKpiResponse> currencies =
                values.entrySet().stream()
                        .map(entry -> entry.getValue().response(entry.getKey()))
                        .toList();
        return new AdminRevenueKpiResponse(ReportingPeriodResponse.from(range), currencies);
    }

    @Override
    public DetailedRevenueReportResponse getDetailedReport(
            ReportingDateRange range, String currency, ReportGrouping grouping) {
        String normalized = calculator.currency(currency);
        ReportGrouping effective = grouping == null ? ReportGrouping.DAY : grouping;
        AdminRevenueKpiResponse total = getKpis(range, normalized);
        Map<Key, Values> values = new HashMap<>();
        Set<String> currencies = new TreeSet<>();
        analytics
                .dailyPayments(range.startInclusive(), range.endExclusive(), normalized)
                .forEach(
                        row -> {
                            currencies.add(row.currency());
                            values.computeIfAbsent(
                                            new Key(
                                                    buckets.key(row.day(), effective),
                                                    row.currency()),
                                            key -> new Values())
                                    .payment(row.count(), row.amount());
                        });
        analytics
                .dailyRefunds(range.startInclusive(), range.endExclusive(), normalized)
                .forEach(
                        row -> {
                            currencies.add(row.currency());
                            values.computeIfAbsent(
                                            new Key(
                                                    buckets.key(row.day(), effective),
                                                    row.currency()),
                                            key -> new Values())
                                    .refund(row.count(), row.amount());
                        });
        if (normalized != null) currencies.add(normalized);
        List<RevenueTrendBucketResponse> trends = new ArrayList<>();
        for (var period : buckets.periods(range.startDate(), range.endDate(), effective)) {
            for (String code : currencies) {
                Values v = values.getOrDefault(new Key(period.key(), code), new Values());
                trends.add(v.response(period.start(), period.end(), code, calculator));
            }
        }
        return new DetailedRevenueReportResponse(
                ReportingPeriodResponse.from(range), effective, total.currencies(), trends);
    }

    private record Key(java.time.LocalDate period, String currency) {}

    private static final class Values {
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal refunds = BigDecimal.ZERO;
        private long payments;
        private long refundCount;

        void payment(long count, BigDecimal amount) {
            payments += count;
            gross = gross.add(amount);
        }

        void refund(long count, BigDecimal amount) {
            refundCount += count;
            refunds = refunds.add(amount);
        }

        RevenueTrendBucketResponse response(
                java.time.LocalDate start,
                java.time.LocalDate end,
                String currency,
                ReportingRateCalculator calculator) {
            return new RevenueTrendBucketResponse(
                    start,
                    end,
                    currency,
                    gross.setScale(2),
                    refunds.setScale(2),
                    gross.subtract(refunds).setScale(2),
                    payments,
                    refundCount,
                    calculator.average(gross, payments));
        }
    }

    private static final class RevenueValues {
        private BigDecimal gross = BigDecimal.ZERO;
        private BigDecimal refunds = BigDecimal.ZERO;
        private long paymentCount;
        private long refundCount;

        private void payment(PaymentRevenueProjection row) {
            gross = amount(row.getAmount());
            paymentCount = row.getCount();
        }

        private void refund(RefundRevenueProjection row) {
            refunds = amount(row.getAmount());
            refundCount = row.getCount();
        }

        private CurrencyRevenueKpiResponse response(String currency) {
            BigDecimal average =
                    paymentCount == 0
                            ? BigDecimal.ZERO.setScale(2)
                            : gross.divide(
                                    BigDecimal.valueOf(paymentCount), 2, RoundingMode.HALF_UP);
            return new CurrencyRevenueKpiResponse(
                    currency,
                    gross.setScale(2, RoundingMode.HALF_UP),
                    refunds.setScale(2, RoundingMode.HALF_UP),
                    gross.subtract(refunds).setScale(2, RoundingMode.HALF_UP),
                    paymentCount,
                    refundCount,
                    average);
        }

        private static BigDecimal amount(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }
}
