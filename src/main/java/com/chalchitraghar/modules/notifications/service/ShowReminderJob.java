package com.chalchitraghar.modules.notifications.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShowReminderJob {
    private final ShowReminderProcessor processor;

    @Scheduled(fixedDelayString = "${app.notifications.reminders.scan-interval:PT5M}")
    public void run() {
        processor.processBatch();
    }
}
