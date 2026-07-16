package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedNotificationDeliveryRetryJob {
    private final NotificationEmailRetryProcessor processor;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.notifications.email.retry-interval-ms:60000}")
    public void retry() {
        try {
            int submitted =
                    jobs.observe(JobName.NOTIFICATION_DELIVERY_RETRY, processor::processBatch);
            if (submitted > 0)
                log.info("Submitted notification email retry batch size={}", submitted);
        } catch (RuntimeException failure) {
            log.error(
                    "Notification email retry batch failed reason={}",
                    failure.getClass().getSimpleName());
        }
    }
}
