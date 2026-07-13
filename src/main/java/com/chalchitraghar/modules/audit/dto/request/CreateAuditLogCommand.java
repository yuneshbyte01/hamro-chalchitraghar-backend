package com.chalchitraghar.modules.audit.dto.request;

import com.chalchitraghar.modules.audit.enums.*;
import java.time.LocalDateTime;
import java.util.Map;

/** Trusted internal command; it is never bound by a controller. */
public record CreateAuditLogCommand(
        LocalDateTime occurredAt,
        Long actorUserId,
        String actorEmailSnapshot,
        String actorRole,
        AuditActorType actorType,
        AuditAction action,
        AuditCategory category,
        AuditSeverity severity,
        String resourceType,
        Long resourceId,
        String resourceReference,
        AuditResult result,
        String failureReason,
        String requestId,
        String correlationId,
        Map<String, ?> beforeValues,
        Map<String, ?> afterValues,
        Map<String, ?> metadata) {}
