package com.chalchitraghar.shared.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.audit.request-context")
public class AuditRequestContextProperties {
    private boolean enabled = true;
    private String requestIdHeader = "X-Request-ID";
    private String correlationIdHeader = "X-Correlation-ID";
    private boolean captureIp = false;
    private boolean maskIp = true;
    private boolean captureUserAgent = true;
    private boolean trustForwardedHeaders = false;
    private int maxRequestIdLength = 100;
    private int maxCorrelationIdLength = 100;
    private int maxUserAgentLength = 512;
}
