package com.chalchitraghar.modules.audit.factory;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.event.*;
import java.time.*;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventFactory {
    private final Clock clock;

    public AuditEvent success(
            String eventId,
            AuditActor actor,
            AuditAction action,
            AuditCategory category,
            AuditSeverity severity,
            String resourceType,
            Long resourceId,
            String reference,
            Map<String, ?> before,
            Map<String, ?> after,
            Map<String, ?> metadata) {
        return build(
                eventId,
                actor,
                action,
                category,
                severity,
                AuditResult.SUCCESS,
                resourceType,
                resourceId,
                reference,
                null,
                before,
                after,
                metadata);
    }

    public AuditEvent attempt(
            AuditActor actor,
            AuditAction action,
            AuditCategory category,
            AuditSeverity severity,
            AuditResult result,
            String resourceType,
            Long resourceId,
            String reference,
            String reason,
            Map<String, ?> metadata) {
        return build(
                action + ":" + UUID.randomUUID(),
                actor,
                action,
                category,
                severity,
                result,
                resourceType,
                resourceId,
                reference,
                reason,
                null,
                null,
                metadata);
    }

    private AuditEvent build(
            String id,
            AuditActor actor,
            AuditAction action,
            AuditCategory category,
            AuditSeverity severity,
            AuditResult result,
            String type,
            Long resourceId,
            String reference,
            String reason,
            Map<String, ?> before,
            Map<String, ?> after,
            Map<String, ?> metadata) {
        return new AuditEvent(
                id,
                LocalDateTime.now(clock),
                actor,
                action,
                category,
                severity,
                result,
                type,
                resourceId,
                reference,
                reason,
                before,
                after,
                metadata);
    }
}
