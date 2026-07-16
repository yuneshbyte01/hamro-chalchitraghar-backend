package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundRetentionJob {
    private final RefundRetentionProcessor processor;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.refunds.retention.interval:PT24H}")
    public void run() {
        jobs.observeAndSuppress(JobName.REFUND_RETENTION, processor::processBatch);
    }
}
