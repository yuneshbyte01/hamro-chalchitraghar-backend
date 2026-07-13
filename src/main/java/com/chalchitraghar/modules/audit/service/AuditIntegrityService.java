package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.entity.AuditLog;
import com.chalchitraghar.modules.audit.enums.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class AuditIntegrityService {
    public String hash(
            LocalDateTime occurredAt,
            Long actorUserId,
            AuditActorType actorType,
            AuditAction action,
            AuditCategory category,
            AuditSeverity severity,
            String resourceType,
            Long resourceId,
            AuditResult result,
            String eventId,
            LocalDateTime createdAt) {
        String canonical =
                String.join(
                        "\u001f",
                        value(occurredAt),
                        value(actorUserId),
                        value(actorType),
                        value(action),
                        value(category),
                        value(severity),
                        value(resourceType),
                        value(resourceId),
                        value(result),
                        value(eventId),
                        value(createdAt));
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }

    public boolean verify(AuditLog a) {
        return MessageDigest.isEqual(
                a.getIntegrityHash().getBytes(StandardCharsets.US_ASCII),
                hash(
                                a.getOccurredAt(),
                                a.getActorUserId(),
                                a.getActorType(),
                                a.getAction(),
                                a.getCategory(),
                                a.getSeverity(),
                                a.getResourceType(),
                                a.getResourceId(),
                                a.getResult(),
                                a.getEventId(),
                                a.getCreatedAt())
                        .getBytes(StandardCharsets.US_ASCII));
    }

    private String value(Object value) {
        if (value instanceof LocalDateTime time) return time.withNano(0).toString();
        return value == null ? "" : value.toString();
    }
}
