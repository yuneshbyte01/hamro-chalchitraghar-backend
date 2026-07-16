package com.chalchitraghar.modules.reporting.service;

import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.AdminRevenueKpiResponse;

public interface RevenueReportService {
    AdminRevenueKpiResponse getKpis(ReportingDateRange range, String currency);
}
