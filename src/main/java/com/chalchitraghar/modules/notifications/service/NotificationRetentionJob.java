package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRetentionJob {
    private final NotificationRetentionProcessor processor;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.notifications.retention.interval:PT24H}")
    public void run() {
        jobs.observe(JobName.NOTIFICATION_RETENTION, processor::processBatch);
    }
}
