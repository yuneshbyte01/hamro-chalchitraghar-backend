package com.chalchitraghar.modules.payments.service;

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

    @Scheduled(fixedDelayString = "${app.refunds.processing.retry-interval:PT1M}")
    public void run() {
        try {
            int recovered = recovery.recover();
            int processed = processor.processBatch();
            if (recovered + processed > 0)
                log.info("Refund maintenance recovered={} processed={}", recovered, processed);
        } catch (RuntimeException e) {
            log.error("Refund maintenance failed reason={}", e.getClass().getSimpleName());
        }
    }
}
