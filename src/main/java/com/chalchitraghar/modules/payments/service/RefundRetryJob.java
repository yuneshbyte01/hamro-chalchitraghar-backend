package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.refunds.processing",
        name = {"enabled", "auto-process-enabled"},
        havingValue = "true")
public class RefundRetryJob {
    private final RefundRetryProcessor processor;
    private final StaleRefundRecoveryService recovery;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.refunds.processing.retry-interval:PT1M}")
    public void run() {
        jobs.observeAndSuppress(
                JobName.REFUND_RETRY, () -> recovery.recover() + processor.processBatch());
    }
}
