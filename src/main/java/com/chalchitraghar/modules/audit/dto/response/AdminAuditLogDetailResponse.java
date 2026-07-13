package com.chalchitraghar.modules.audit.dto.response;

import com.chalchitraghar.modules.audit.enums.*;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public record AdminAuditLogDetailResponse(
        Long id,
        LocalDateTime occurredAt,
        AuditActorType actorType,
        Long actorUserId,
        String actorEmail,
        String actorRole,
        AuditAction action,
        AuditCategory category,
        AuditSeverity severity,
        AuditResult result,
        String resourceType,
        String resourceReference,
        String requestId,
        String correlationId,
        String maskedIpAddress,
        String userAgent,
        String httpMethod,
        String requestPath,
        Long resourceId,
        String failureReason,
        JsonNode beforeValues,
        JsonNode afterValues,
        JsonNode metadata,
        LocalDateTime createdAt) {}
