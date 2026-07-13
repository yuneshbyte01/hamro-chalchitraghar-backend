package com.chalchitraghar.modules.audit.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditRetentionJob {
    private final AuditRetentionService retention;

    @Scheduled(fixedDelayString = "${app.audit.retention.interval:PT24H}")
    public void run() {
        try {
            int count = retention.processBatch();
            if (count > 0) log.info("Audit retention anonymized {} rows", count);
        } catch (RuntimeException ex) {
            log.error("Audit retention batch failed", ex);
        }
    }
}
