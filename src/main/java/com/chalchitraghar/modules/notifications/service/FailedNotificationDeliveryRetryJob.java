package com.chalchitraghar.modules.notifications.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailedNotificationDeliveryRetryJob {
    private final NotificationEmailRetryProcessor processor;

    @Scheduled(fixedDelayString = "${app.notifications.email.retry-interval-ms:60000}")
    public void retry() {
        try {
            int submitted = processor.processBatch();
            if (submitted > 0)
                log.info("Submitted notification email retry batch size={}", submitted);
        } catch (RuntimeException failure) {
            log.error(
                    "Notification email retry batch failed reason={}",
                    failure.getClass().getSimpleName());
        }
    }
}
