package com.chalchitraghar.modules.audit.service.impl;

import com.chalchitraghar.modules.audit.dto.request.*;
import com.chalchitraghar.modules.audit.dto.response.*;
import com.chalchitraghar.modules.audit.entity.AuditLog;
import com.chalchitraghar.modules.audit.mapper.AuditLogMapper;
import com.chalchitraghar.modules.audit.repository.AuditLogRepository;
import com.chalchitraghar.modules.audit.service.AuditLogService;
import com.chalchitraghar.modules.audit.specification.AuditLogSpecification;
import com.chalchitraghar.modules.audit.validation.AuditSnapshotValidator;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.observability.BusinessMetric;
import com.chalchitraghar.shared.observability.BusinessMetrics;
import com.chalchitraghar.shared.observability.BusinessOperation;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.*;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {
    private final AuditLogRepository repository;
    private final AuditSnapshotValidator snapshots;
    private final AuditLogMapper mapper;
    private final com.chalchitraghar.modules.audit.service.AuditIntegrityService integrity;
    private final Clock clock;
    private final BusinessMetrics metrics;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AdminAuditLogDetailResponse append(CreateAuditLogCommand c) {
        return metrics.observe(
                BusinessMetric.AUDIT, BusinessOperation.APPEND, () -> appendObserved(c));
    }

    private AdminAuditLogDetailResponse appendObserved(CreateAuditLogCommand c) {
        if (c == null) throw new IllegalArgumentException("Audit command is required");
        require(c.actorType(), "Actor type");
        require(c.action(), "Action");
        require(c.category(), "Category");
        require(c.severity(), "Severity");
        require(c.result(), "Result");
        String resourceType = bounded(c.resourceType(), 100, true, "Resource type");
        LocalDateTime now = LocalDateTime.now(clock);
        String eventId = bounded(c.eventId(), 200, false, "Event ID");
        LocalDateTime occurredAt = c.occurredAt() == null ? now : c.occurredAt();
        AuditLog log =
                new AuditLog(
                        null,
                        eventId,
                        occurredAt,
                        c.actorUserId(),
                        email(c.actorEmailSnapshot()),
                        bounded(c.actorRole(), 50, false, "Actor role"),
                        c.actorType(),
                        c.action(),
                        c.category(),
                        c.severity(),
                        resourceType.toUpperCase(Locale.ROOT),
                        c.resourceId(),
                        bounded(c.resourceReference(), 200, false, "Resource reference"),
                        c.result(),
                        failureReason(c.failureReason()),
                        bounded(c.requestId(), 100, false, "Request ID"),
                        bounded(c.correlationId(), 100, false, "Correlation ID"),
                        bounded(c.ipAddress(), 100, false, "IP address"),
                        bounded(c.userAgent(), 512, false, "User agent"),
                        bounded(c.httpMethod(), 16, false, "HTTP method"),
                        bounded(c.requestPath(), 500, false, "Request path"),
                        snapshots.validateAndSerialize(c.beforeValues(), "beforeValues"),
                        snapshots.validateAndSerialize(c.afterValues(), "afterValues"),
                        snapshots.validateAndSerialize(c.metadata(), "metadata"),
                        now,
                        com.chalchitraghar.modules.audit.enums.AuditRetentionStatus.ACTIVE,
                        null,
                        integrity.hash(
                                occurredAt,
                                c.actorUserId(),
                                c.actorType(),
                                c.action(),
                                c.category(),
                                c.severity(),
                                resourceType.toUpperCase(Locale.ROOT),
                                c.resourceId(),
                                c.result(),
                                eventId,
                                now));
        return mapper.toAdminDetail(repository.save(log));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogSummaryResponse> findAll(
            AdminAuditLogFilterRequest filters, int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException(
                    "Page must be non-negative and size must be 1 to 100");
        if (filters.occurredFrom() != null
                && filters.occurredTo() != null
                && filters.occurredFrom().isAfter(filters.occurredTo()))
            throw new IllegalArgumentException("Invalid date range");
        if (filters.createdFrom() != null
                && filters.createdTo() != null
                && filters.createdFrom().isAfter(filters.createdTo()))
            throw new IllegalArgumentException("Invalid created date range");
        Page<AuditLog> result =
                repository.findAll(
                        AuditLogSpecification.matching(filters),
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
        return PageResponse.from(result, result.stream().map(mapper::toAdminSummary).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminAuditLogDetailResponse findById(Long id) {
        return mapper.toAdminDetail(
                repository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Audit log", id)));
    }

    private void require(Object value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
    }

    private String email(String value) {
        String result = bounded(value, 320, false, "Actor email");
        return result == null ? null : result.toLowerCase(Locale.ROOT);
    }

    private String bounded(String value, int max, boolean required, String name) {
        if (value == null || value.isBlank()) {
            if (required) throw new IllegalArgumentException(name + " is required");
            return null;
        }
        String result = value.trim();
        if (result.length() > max) throw new IllegalArgumentException(name + " is too long");
        return result;
    }

    private String failureReason(String value) {
        String result = bounded(value, 500, false, "Failure reason");
        if (result == null) return null;
        result =
                result.replaceAll("[\\r\\n\\t]+", " ")
                        .replaceAll(
                                "(?i)(password|token|secret|credential)\\s*[:=]\\s*\\S+",
                                "$1=[REDACTED]");
        return result.length() <= 500 ? result : result.substring(0, 500);
    }
}
