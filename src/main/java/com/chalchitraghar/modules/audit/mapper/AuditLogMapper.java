package com.chalchitraghar.modules.audit.mapper;

import com.chalchitraghar.modules.audit.dto.response.*;
import com.chalchitraghar.modules.audit.entity.AuditLog;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogMapper {
    private final ObjectMapper objectMapper;

    public AdminAuditLogSummaryResponse toAdminSummary(AuditLog a) {
        return new AdminAuditLogSummaryResponse(
                a.getId(),
                a.getOccurredAt(),
                a.getActorType(),
                a.getActorUserId(),
                mask(a.getActorEmailSnapshot()),
                a.getActorRole(),
                a.getAction(),
                a.getCategory(),
                a.getSeverity(),
                a.getResult(),
                a.getResourceType(),
                a.getResourceReference(),
                a.getRequestId(),
                a.getCorrelationId());
    }

    public AdminAuditLogDetailResponse toAdminDetail(AuditLog a) {
        return new AdminAuditLogDetailResponse(
                a.getId(),
                a.getOccurredAt(),
                a.getActorType(),
                a.getActorUserId(),
                mask(a.getActorEmailSnapshot()),
                a.getActorRole(),
                a.getAction(),
                a.getCategory(),
                a.getSeverity(),
                a.getResult(),
                a.getResourceType(),
                a.getResourceReference(),
                a.getRequestId(),
                a.getCorrelationId(),
                a.getResourceId(),
                a.getFailureReason(),
                json(a.getBeforeValues()),
                json(a.getAfterValues()),
                json(a.getMetadata()),
                a.getCreatedAt());
    }

    private JsonNode json(String value) {
        try {
            return value == null ? null : objectMapper.readTree(value);
        } catch (Exception e) {
            throw new IllegalStateException("Stored audit JSON is invalid");
        }
    }

    private String mask(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        return at < 0 ? "***" : email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }
}
