package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.modules.reporting.dto.request.ReportGrouping;
import java.util.List;

public record DetailedRevenueReportResponse(
        ReportingPeriodResponse period,
        ReportGrouping groupBy,
        List<CurrencyRevenueKpiResponse> totals,
        List<RevenueTrendBucketResponse> trends) {}
