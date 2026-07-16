package com.chalchitraghar.modules.reporting.service;

import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.AdminDashboardSummaryResponse;

public interface DashboardReportService {
    AdminDashboardSummaryResponse getSummary(ReportingDateRange range, String currency);
}
