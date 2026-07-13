package com.chalchitraghar.modules.audit.dto.request;

import com.chalchitraghar.modules.audit.enums.*;
import java.time.LocalDateTime;

public record AdminAuditLogFilterRequest(
        Long actorUserId,
        String actorEmail,
        String actorRole,
        AuditActorType actorType,
        AuditAction action,
        AuditCategory category,
        AuditSeverity severity,
        AuditResult result,
        String resourceType,
        Long resourceId,
        String resourceReference,
        String requestId,
        String correlationId,
        LocalDateTime occurredFrom,
        LocalDateTime occurredTo) {}
