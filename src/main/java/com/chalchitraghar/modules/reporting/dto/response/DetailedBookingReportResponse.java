package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.modules.reporting.dto.request.ReportGrouping;
import java.util.List;

public record DetailedBookingReportResponse(
        AdminBookingKpiResponse totals,
        ReportGrouping groupBy,
        List<BookingTrendBucketResponse> trends) {}
