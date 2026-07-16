package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefundRetentionJob {
    private final RefundRetentionProcessor processor;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.refunds.retention.interval:PT24H}")
    public void run() {
        try {
            int count = jobs.observe(JobName.REFUND_RETENTION, processor::processBatch);
            if (count > 0) log.info("Refund retention processed={}", count);
        } catch (RuntimeException e) {
            log.error("Refund retention failed reason={}", e.getClass().getSimpleName());
        }
    }
}
