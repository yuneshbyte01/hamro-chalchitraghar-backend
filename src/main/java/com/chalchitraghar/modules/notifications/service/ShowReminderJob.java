package com.chalchitraghar.modules.notifications.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ShowReminderJob {
    private final ShowReminderProcessor processor;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.notifications.reminders.scan-interval:PT5M}")
    public void run() {
        jobs.observe(JobName.SHOW_REMINDER, processor::processBatch);
    }
}
