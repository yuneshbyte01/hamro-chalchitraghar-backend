package com.chalchitraghar.modules.reporting.schedule;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ScheduledReportDispatcher {
    private final ScheduledReportService reports;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.reporting.schedule.scan-interval:PT5M}")
    public void dispatch() {
        jobs.observe(JobName.SCHEDULED_REPORT_DISPATCH, reports::dispatchDue);
    }

    @Scheduled(fixedDelayString = "${app.reporting.schedule.retry-delay:PT5M}")
    public void retry() {
        jobs.observe(JobName.SCHEDULED_REPORT_RETRY, reports::retryFailed);
    }
}
