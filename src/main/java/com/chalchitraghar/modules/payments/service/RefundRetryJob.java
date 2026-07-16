package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
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
        try {
            int[] counts = new int[2];
            int total =
                    jobs.observe(
                            JobName.REFUND_RETRY,
                            () -> {
                                counts[0] = recovery.recover();
                                counts[1] = processor.processBatch();
                                return counts[0] + counts[1];
                            });
            int recovered = counts[0];
            int processed = counts[1];
            if (total > 0)
                log.info("Refund maintenance recovered={} processed={}", recovered, processed);
        } catch (RuntimeException e) {
            log.error("Refund maintenance failed reason={}", e.getClass().getSimpleName());
        }
    }
}
