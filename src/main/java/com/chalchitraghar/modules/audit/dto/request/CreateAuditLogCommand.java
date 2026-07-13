package com.chalchitraghar.modules.audit.dto.request;

import com.chalchitraghar.modules.audit.enums.*;
import java.time.LocalDateTime;
import java.util.Map;

/** Trusted internal command; it is never bound by a controller. */
public record CreateAuditLogCommand(
        String eventId,
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
        String ipAddress,
        String userAgent,
        String httpMethod,
        String requestPath,
        Map<String, ?> beforeValues,
        Map<String, ?> afterValues,
        Map<String, ?> metadata) {
    public CreateAuditLogCommand(
            String eventId,
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
            Map<String, ?> metadata) {
        this(
                eventId,
                occurredAt,
                actorUserId,
                actorEmailSnapshot,
                actorRole,
                actorType,
                action,
                category,
                severity,
                resourceType,
                resourceId,
                resourceReference,
                result,
                failureReason,
                requestId,
                correlationId,
                null,
                null,
                null,
                null,
                beforeValues,
                afterValues,
                metadata);
    }
}
