package com.chalchitraghar.shared.audit;

import java.time.LocalDateTime;

public record RequestAuditContext(
        String requestId,
        String correlationId,
        String ipAddress,
        String userAgent,
        String httpMethod,
        String requestPath,
        String source,
        LocalDateTime createdAt) {}
