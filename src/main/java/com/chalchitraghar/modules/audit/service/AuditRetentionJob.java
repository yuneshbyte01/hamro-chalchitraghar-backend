package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRetentionJob {
    private final AuditRetentionService retention;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.audit.retention.interval:PT24H}")
    public void run() {
        try {
            int count = jobs.observe(JobName.AUDIT_RETENTION, retention::processBatch);
            if (count > 0) log.info("Audit retention anonymized {} rows", count);
        } catch (RuntimeException ex) {
            log.error("Audit retention batch failed", ex);
        }
    }
}
