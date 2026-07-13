package com.chalchitraghar.modules.audit.dto.response;

import com.chalchitraghar.modules.audit.enums.*;
import java.time.LocalDateTime;

public record AdminAuditLogSummaryResponse(
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
        String httpMethod,
        String requestPath) {}
