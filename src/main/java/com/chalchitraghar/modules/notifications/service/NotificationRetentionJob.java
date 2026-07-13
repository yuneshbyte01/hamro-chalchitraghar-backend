package com.chalchitraghar.modules.notifications.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationRetentionJob {
    private final NotificationRetentionProcessor processor;

    @Scheduled(fixedDelayString = "${app.notifications.retention.interval:PT24H}")
    public void run() {
        processor.processBatch();
    }
}
