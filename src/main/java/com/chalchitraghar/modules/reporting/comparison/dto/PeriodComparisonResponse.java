package com.chalchitraghar.modules.reporting.comparison.dto;

import com.chalchitraghar.modules.reporting.dto.response.ReportingPeriodResponse;
import java.util.List;

public record PeriodComparisonResponse(
        ReportingPeriodResponse currentPeriod,
        ReportingPeriodResponse comparisonPeriod,
        List<CurrencyPeriodComparisonResponse> revenue,
        MetricComparisonResponse totalBookings,
        MetricComparisonResponse confirmedBookings,
        MetricComparisonResponse cancelledBookings,
        MetricComparisonResponse expiredBookings,
        MetricComparisonResponse registeredCustomers,
        MetricComparisonResponse confirmationRate,
        MetricComparisonResponse cancellationRate) {}
