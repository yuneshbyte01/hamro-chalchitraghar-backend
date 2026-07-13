package com.chalchitraghar.modules.audit.event;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.shared.audit.RequestAuditContext;
import java.time.LocalDateTime;
import java.util.Map;

/** Immutable, entity-free representation of one logical audit event. */
public record AuditEvent(
        String eventId,
        LocalDateTime occurredAt,
        AuditActor actor,
        AuditAction action,
        AuditCategory category,
        AuditSeverity severity,
        AuditResult result,
        String resourceType,
        Long resourceId,
        String resourceReference,
        String failureReason,
        Map<String, ?> beforeValues,
        Map<String, ?> afterValues,
        Map<String, ?> metadata,
        RequestAuditContext requestContext) {
    public AuditEvent(
            String eventId,
            LocalDateTime occurredAt,
            AuditActor actor,
            AuditAction action,
            AuditCategory category,
            AuditSeverity severity,
            AuditResult result,
            String resourceType,
            Long resourceId,
            String resourceReference,
            String failureReason,
            Map<String, ?> beforeValues,
            Map<String, ?> afterValues,
            Map<String, ?> metadata) {
        this(
                eventId,
                occurredAt,
                actor,
                action,
                category,
                severity,
                result,
                resourceType,
                resourceId,
                resourceReference,
                failureReason,
                beforeValues,
                afterValues,
                metadata,
                com.chalchitraghar.shared.audit.RequestAuditContextHolder.current().orElse(null));
    }
}
