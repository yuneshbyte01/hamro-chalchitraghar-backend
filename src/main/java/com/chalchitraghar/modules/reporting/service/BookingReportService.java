package com.chalchitraghar.modules.reporting.service;

import com.chalchitraghar.modules.reporting.dto.request.ReportGrouping;
import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.AdminBookingKpiResponse;
import com.chalchitraghar.modules.reporting.dto.response.DetailedBookingReportResponse;

public interface BookingReportService {
    AdminBookingKpiResponse getKpis(ReportingDateRange range);

    DetailedBookingReportResponse getDetailedReport(
            ReportingDateRange range, ReportGrouping grouping);
}
