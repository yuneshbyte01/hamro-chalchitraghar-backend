package com.chalchitraghar.modules.notifications.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.notifications.email")
public class NotificationEmailProperties {
    private boolean enabled;
    private String from;
    private String fromName = "Hamro Chalchitraghar";

    @Min(1)
    private int maxAttempts = 3;

    @Min(1)
    private long initialRetryDelayMs = 60_000;

    @DecimalMin("1.0")
    private double retryMultiplier = 2.0;

    @Min(1)
    private long maxRetryDelayMs = 3_600_000;

    @Min(1)
    private int retryBatchSize = 50;

    @Min(1)
    private long retryIntervalMs = 60_000;

    @Min(1)
    private long processingTimeoutMs = 300_000;

    @Valid private Async async = new Async();
    @Valid private Executor executor = new Executor();

    @AssertTrue(message = "app.notifications.email.from is required when email is enabled")
    public boolean isValidEnabledConfiguration() {
        return !enabled || from != null && !from.isBlank();
    }

    @Getter
    @Setter
    public static class Async {
        private boolean enabled = true;
    }

    @Getter
    @Setter
    public static class Executor {
        @Min(1)
        private int corePoolSize = 2;

        @Min(1)
        private int maxPoolSize = 4;

        @Min(0)
        private int queueCapacity = 100;

        @Min(0)
        private int keepAliveSeconds = 60;

        @AssertTrue(message = "executor max pool size must be at least core pool size")
        public boolean isValidPoolRange() {
            return maxPoolSize >= corePoolSize;
        }
    }
}
