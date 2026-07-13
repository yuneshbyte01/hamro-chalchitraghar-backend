package com.chalchitraghar.modules.tickets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tickets")
public record TicketOperationsProperties(
        long expiryReconciliationIntervalMs, int expiryBatchSize, boolean pdfEnabled, Email email) {
    public TicketOperationsProperties {
        if (expiryReconciliationIntervalMs < 1 || expiryBatchSize < 1)
            throw new IllegalArgumentException("Ticket expiry configuration must be positive");
        if (email == null)
            throw new IllegalArgumentException("Ticket email configuration is required");
    }

    public record Email(
            boolean enabled,
            String from,
            int maxAttempts,
            long retryIntervalMs,
            int retryBatchSize) {
        public Email {
            if (enabled && (from == null || from.isBlank()))
                throw new IllegalArgumentException(
                        "Ticket email sender is required when delivery is enabled");
            if (maxAttempts < 1 || retryIntervalMs < 1 || retryBatchSize < 1)
                throw new IllegalArgumentException(
                        "Ticket email retry configuration must be positive");
        }
    }
}
