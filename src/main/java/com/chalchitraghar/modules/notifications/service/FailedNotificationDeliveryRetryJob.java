package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FailedNotificationDeliveryRetryJob {
    private final NotificationEmailRetryProcessor processor;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.notifications.email.retry-interval-ms:60000}")
    public void retry() {
        jobs.observeAndSuppress(JobName.NOTIFICATION_DELIVERY_RETRY, processor::processBatch);
    }
}
