package com.chalchitraghar.modules.reporting.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.reporting")
public record ReportingOperationsProperties(Export export, Schedule schedule) {
    public ReportingOperationsProperties {
        export = export == null ? new Export(50_000, 20_000_000) : export;
        schedule = schedule == null ? new Schedule(25, 5, Duration.ofMinutes(5), false) : schedule;
    }

    public record Export(int maxRows, long maxFileSizeBytes) {}

    public record Schedule(
            int batchSize, int maxAttempts, Duration retryDelay, boolean emailEnabled) {}
}
