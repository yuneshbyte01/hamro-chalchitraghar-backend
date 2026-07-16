package com.chalchitraghar.modules.reporting.service;

import com.chalchitraghar.modules.reporting.dto.request.ReportGrouping;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.AdminRevenueKpiResponse;
import com.chalchitraghar.modules.reporting.dto.response.DetailedRevenueReportResponse;

public interface RevenueReportService {
    AdminRevenueKpiResponse getKpis(ReportingDateRange range, String currency);

    DetailedRevenueReportResponse getDetailedReport(
            ReportingDateRange range, String currency, ReportGrouping grouping);
}
