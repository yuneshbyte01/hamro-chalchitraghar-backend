package com.chalchitraghar.modules.reporting.service.impl;

import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.*;
import com.chalchitraghar.modules.reporting.projection.*;
import com.chalchitraghar.modules.reporting.repository.ReportingQueryRepository;
import com.chalchitraghar.modules.reporting.service.RevenueReportService;
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

    @Override
    public AdminRevenueKpiResponse getKpis(ReportingDateRange range, String currency) {
        String normalized = normalizeCurrency(currency);
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

    private String normalizeCurrency(String currency) {
        if (currency == null) return null;
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currency must contain exactly three letters");
        }
        return normalized;
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
