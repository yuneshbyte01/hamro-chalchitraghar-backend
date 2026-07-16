package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditRetentionJob {
    private final AuditRetentionService retention;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.audit.retention.interval:PT24H}")
    public void run() {
        jobs.observeAndSuppress(JobName.AUDIT_RETENTION, retention::processBatch);
    }
}
