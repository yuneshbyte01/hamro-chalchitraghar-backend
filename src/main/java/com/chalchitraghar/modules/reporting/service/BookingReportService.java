package com.chalchitraghar.modules.reporting.service;

import com.chalchitraghar.modules.reporting.dto.request.ReportingDateRange;
import com.chalchitraghar.modules.reporting.dto.response.AdminBookingKpiResponse;

public interface BookingReportService {
    AdminBookingKpiResponse getKpis(ReportingDateRange range);
}
