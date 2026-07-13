package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.config.AuditOperationsProperties;
import com.chalchitraghar.modules.audit.dto.request.AdminAuditLogFilterRequest;
import com.chalchitraghar.modules.audit.dto.response.*;
import com.chalchitraghar.modules.audit.entity.AuditLog;
import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.event.AuditEvent;
import com.chalchitraghar.modules.audit.factory.AuditActorResolver;
import com.chalchitraghar.modules.audit.listener.AuditEventListener;
import com.chalchitraghar.modules.audit.mapper.AuditLogMapper;
import com.chalchitraghar.modules.audit.repository.AuditLogRepository;
import com.chalchitraghar.modules.audit.specification.AuditLogSpecification;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditReportService {
    private final AuditLogRepository repository;
    private final AuditLogMapper mapper;
    private final AuditOperationsProperties properties;
    private final AuditIntegrityService integrity;
    private final Clock clock;
    private final AuditActorResolver actors;
    private final AuditEventListener events;

    @Transactional(readOnly = true)
    public List<FailedLoginBucketResponse> failedLogins(
            LocalDateTime from, LocalDateTime to, AuditReportBucket bucket, int minimumCount) {
        validateRange(from, to);
        var rows =
                load(
                        rangeFilter(from, to, AuditAction.LOGIN_FAILED, null, null),
                        properties.getExport().getMaxRows());
        ZoneId zone = clock.getZone();
        Map<LocalDateTime, List<AuditLog>> grouped =
                rows.stream()
                        .collect(
                                Collectors.groupingBy(
                                        a ->
                                                truncate(a.getOccurredAt().atZone(zone), bucket)
                                                        .toLocalDateTime(),
                                        TreeMap::new,
                                        Collectors.toList()));
        return grouped.entrySet().stream()
                .filter(e -> e.getValue().size() >= Math.max(1, minimumCount))
                .map(e -> failedBucket(e.getKey(), e.getValue()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAuditLogSummaryResponse> highRisk(
            LocalDateTime from,
            LocalDateTime to,
            AuditCategory category,
            AuditAction action,
            AuditResult result,
            int page,
            int size) {
        validateRange(from, to);
        if (page < 0 || size < 1 || size > 100)
            throw new IllegalArgumentException("Invalid pagination");
        var f = rangeFilter(from, to, action, category, result);
        f =
                new AdminAuditLogFilterRequest(
                        f.actorUserId(),
                        f.actorEmail(),
                        f.actorRole(),
                        f.actorType(),
                        f.action(),
                        f.category(),
                        f.severity(),
                        f.result(),
                        f.resourceType(),
                        f.resourceId(),
                        f.resourceReference(),
                        f.requestId(),
                        f.correlationId(),
                        f.httpMethod(),
                        f.requestPath(),
                        f.occurredFrom(),
                        f.occurredTo(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        true);
        Page<AuditLog> rows =
                repository.findAll(
                        AuditLogSpecification.matching(f),
                        PageRequest.of(
                                page,
                                size,
                                Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
        return PageResponse.from(rows, rows.stream().map(mapper::toAdminSummary).toList());
    }

    @Transactional(readOnly = true)
    public AuditSummaryReportResponse summary(LocalDateTime from, LocalDateTime to) {
        validateRange(from, to);
        long total = repository.countByOccurredAtBetween(from, to);
        Map<String, Long> category = aggregate(repository.countCategories(from, to));
        Map<String, Long> severity = aggregate(repository.countSeverities(from, to));
        Map<String, Long> actors = aggregate(repository.countActorTypes(from, to));
        Map<String, Long> actions =
                aggregate(repository.countActions(from, to, PageRequest.of(0, 10)));
        return new AuditSummaryReportResponse(
                total,
                repository.countByResultAndOccurredAtBetween(AuditResult.SUCCESS, from, to),
                repository.countByResultAndOccurredAtBetween(AuditResult.FAILURE, from, to),
                repository.countByResultAndOccurredAtBetween(AuditResult.DENIED, from, to),
                category,
                severity,
                actors,
                actions,
                repository.countByActorTypeAndOccurredAtBetween(AuditActorType.SYSTEM, from, to),
                repository.countByActorTypeAndOccurredAtBetween(AuditActorType.EXTERNAL, from, to));
    }

    @Transactional(readOnly = true)
    public AuditIntegrityCheckResponse integrity(LocalDateTime from, LocalDateTime to) {
        if (!properties.getIntegrity().isEnabled())
            throw new IllegalArgumentException("Audit integrity verification is disabled");
        validateRange(from, to);
        var rows =
                load(
                        rangeFilter(from, to, null, null, null),
                        properties.getIntegrity().getBatchSize());
        long mismatches =
                rows.stream()
                        .filter(a -> a.getIntegrityHash() == null || !integrity.verify(a))
                        .count();
        events.persist(
                new AuditEvent(
                        "AUDIT_INTEGRITY_CHECKED:" + UUID.randomUUID(),
                        LocalDateTime.now(clock),
                        actors.currentUserOrSystem(),
                        AuditAction.AUDIT_INTEGRITY_CHECKED,
                        AuditCategory.SECURITY,
                        AuditSeverity.HIGH,
                        AuditResult.SUCCESS,
                        "AUDIT_LOG",
                        null,
                        null,
                        null,
                        null,
                        null,
                        Map.of(
                                "count",
                                rows.size(),
                                "result",
                                mismatches == 0 ? "VALID" : "MISMATCH")));
        return new AuditIntegrityCheckResponse(rows.size(), mismatches);
    }

    private List<AuditLog> load(AdminAuditLogFilterRequest f, int max) {
        Page<AuditLog> page =
                repository.findAll(
                        AuditLogSpecification.matching(f),
                        PageRequest.of(
                                0,
                                max,
                                Sort.by(Sort.Order.asc("occurredAt"), Sort.Order.asc("id"))));
        if (page.hasNext())
            throw new IllegalArgumentException("Report result exceeds safe processing limit");
        return page.getContent();
    }

    private void validateRange(LocalDateTime from, LocalDateTime to) {
        if (from == null || to == null || from.isAfter(to))
            throw new IllegalArgumentException("A valid date range is required");
        if (Duration.between(from, to)
                        .compareTo(Duration.ofDays(properties.getReports().getMaxRangeDays()))
                > 0) throw new IllegalArgumentException("Report date range is too large");
    }

    private AdminAuditLogFilterRequest rangeFilter(
            LocalDateTime from,
            LocalDateTime to,
            AuditAction action,
            AuditCategory category,
            AuditResult result) {
        return new AdminAuditLogFilterRequest(
                null, null, null, null, action, category, null, result, null, null, null, null,
                null, null, null, from, to);
    }

    private ZonedDateTime truncate(ZonedDateTime value, AuditReportBucket bucket) {
        value = value.withMinute(0).withSecond(0).withNano(0);
        return bucket == AuditReportBucket.DAY ? value.withHour(0) : value;
    }

    private FailedLoginBucketResponse failedBucket(LocalDateTime at, List<AuditLog> rows) {
        Set<String> emails =
                rows.stream()
                        .map(AuditLog::getActorEmailSnapshot)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        return new FailedLoginBucketResponse(
                at,
                rows.size(),
                emails.size(),
                reasons(rows, "lock"),
                reasons(rows, "credential"),
                reasons(rows, "locked"),
                reasons(rows, "disabled"),
                rows.stream().filter(a -> a.getActorType() == AuditActorType.ANONYMOUS).count());
    }

    private long reasons(List<AuditLog> rows, String value) {
        return rows.stream()
                .filter(
                        a ->
                                a.getFailureReason() != null
                                        && a.getFailureReason()
                                                .toLowerCase(Locale.ROOT)
                                                .contains(value))
                .count();
    }

    private Map<String, Long> aggregate(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        rows.forEach(row -> result.put(row[0].toString(), ((Number) row[1]).longValue()));
        return result;
    }
}
