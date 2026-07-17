package com.chalchitraghar.modules.reporting.schedule;

import com.chalchitraghar.modules.reporting.schedule.dto.*;
import com.chalchitraghar.shared.response.PageResponse;
import java.util.List;

public interface ScheduledReportService {
    ScheduledReportResponse create(ScheduledReportRequest request);

    List<ScheduledReportResponse> list();

    ScheduledReportResponse get(long id);

    ScheduledReportResponse update(long id, ScheduledReportRequest request);

    ScheduledReportResponse enable(long id, boolean enabled);

    void delete(long id);

    ReportDeliveryResponse run(long id);

    int dispatchDue();

    int retryFailed();

    PageResponse<ReportDeliveryResponse> deliveries(int page, int size);

    ReportDeliveryResponse delivery(long id);
}
