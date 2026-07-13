package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.config.AuditOperationsProperties;
import com.chalchitraghar.modules.audit.entity.AuditLog;
import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.repository.AuditLogRepository;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditRetentionService {
    private final AuditLogRepository repository;
    private final AuditOperationsProperties properties;
    private final Clock clock;

    @Transactional
    public int processBatch() {
        if (!properties.getRetention().isEnabled()) return 0;
        LocalDateTime now = LocalDateTime.now(clock);
        var config = properties.getRetention();
        Specification<AuditLog> eligible =
                (root, query, cb) -> {
                    var active = cb.equal(root.get("retentionStatus"), AuditRetentionStatus.ACTIVE);
                    var ordinary =
                            cb.and(
                                    cb.not(
                                            root.get("category")
                                                    .in(
                                                            AuditCategory.SECURITY,
                                                            AuditCategory.PAYMENT,
                                                            AuditCategory.TICKET)),
                                    cb.not(
                                            root.get("severity")
                                                    .in(
                                                            AuditSeverity.HIGH,
                                                            AuditSeverity.CRITICAL)),
                                    cb.lessThan(
                                            root.get("occurredAt"),
                                            now.minusDays(config.getDefaultDays())));
                    var security =
                            cb.and(
                                    root.get("category").in(AuditCategory.SECURITY),
                                    cb.lessThan(
                                            root.get("occurredAt"),
                                            now.minusDays(config.getSecurityDays())));
                    var payment =
                            cb.and(
                                    root.get("category")
                                            .in(AuditCategory.PAYMENT, AuditCategory.TICKET),
                                    cb.lessThan(
                                            root.get("occurredAt"),
                                            now.minusDays(config.getPaymentDays())));
                    var high =
                            cb.and(
                                    root.get("severity")
                                            .in(AuditSeverity.HIGH, AuditSeverity.CRITICAL),
                                    cb.lessThan(
                                            root.get("occurredAt"),
                                            now.minusDays(config.getHighSeverityDays())));
                    return cb.and(active, cb.or(ordinary, security, payment, high));
                };
        Page<AuditLog> batch =
                repository.findAll(
                        eligible,
                        PageRequest.of(
                                0,
                                config.getBatchSize(),
                                Sort.by(Sort.Order.asc("occurredAt"), Sort.Order.asc("id"))));
        batch.forEach(row -> row.anonymize(now));
        return batch.getNumberOfElements();
    }
}
