package com.chalchitraghar.modules.payments.config;

import jakarta.validation.constraints.*;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.payments.esewa")
public record EsewaProperties(
        boolean enabled,
        @NotBlank String environment,
        @NotBlank String productCode,
        @NotBlank String secretKey,
        @NotNull URI paymentUrl,
        @NotNull URI statusCheckUrl,
        @NotNull URI successUrl,
        @NotNull URI failureUrl,
        @Positive int connectTimeoutMs,
        @Positive int readTimeoutMs) {}
