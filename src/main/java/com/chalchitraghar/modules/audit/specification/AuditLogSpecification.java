package com.chalchitraghar.modules.audit.specification;

import com.chalchitraghar.modules.audit.dto.request.AdminAuditLogFilterRequest;
import com.chalchitraghar.modules.audit.entity.AuditLog;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class AuditLogSpecification {
    private AuditLogSpecification() {}

    public static Specification<AuditLog> matching(AdminAuditLogFilterRequest c) {
        return (root, query, cb) -> {
            var p = new ArrayList<jakarta.persistence.criteria.Predicate>();
            if (c.actorUserId() != null) p.add(cb.equal(root.get("actorUserId"), c.actorUserId()));
            contains(cb, p, root.get("actorEmailSnapshot"), c.actorEmail());
            equalText(cb, p, root.get("actorRole"), c.actorRole());
            if (c.actorType() != null) p.add(cb.equal(root.get("actorType"), c.actorType()));
            if (c.action() != null) p.add(cb.equal(root.get("action"), c.action()));
            if (c.category() != null) p.add(cb.equal(root.get("category"), c.category()));
            if (c.severity() != null) p.add(cb.equal(root.get("severity"), c.severity()));
            if (c.result() != null) p.add(cb.equal(root.get("result"), c.result()));
            equalText(cb, p, root.get("resourceType"), c.resourceType());
            if (c.resourceId() != null) p.add(cb.equal(root.get("resourceId"), c.resourceId()));
            contains(cb, p, root.get("resourceReference"), c.resourceReference());
            equalText(cb, p, root.get("requestId"), c.requestId());
            equalText(cb, p, root.get("correlationId"), c.correlationId());
            equalText(cb, p, root.get("httpMethod"), c.httpMethod());
            if (c.requestPath() != null && !c.requestPath().isBlank())
                p.add(cb.like(root.get("requestPath"), escape(c.requestPath().trim()) + "%", '\\'));
            if (c.occurredFrom() != null)
                p.add(cb.greaterThanOrEqualTo(root.get("occurredAt"), c.occurredFrom()));
            if (c.occurredTo() != null)
                p.add(cb.lessThanOrEqualTo(root.get("occurredAt"), c.occurredTo()));
            if (c.createdFrom() != null)
                p.add(cb.greaterThanOrEqualTo(root.get("createdAt"), c.createdFrom()));
            if (c.createdTo() != null)
                p.add(cb.lessThanOrEqualTo(root.get("createdAt"), c.createdTo()));
            present(cb, p, root.get("beforeValues"), c.hasBeforeValues());
            present(cb, p, root.get("afterValues"), c.hasAfterValues());
            present(cb, p, root.get("metadata"), c.hasMetadata());
            if (Boolean.TRUE.equals(c.systemOnly()))
                p.add(
                        cb.equal(
                                root.get("actorType"),
                                com.chalchitraghar.modules.audit.enums.AuditActorType.SYSTEM));
            if (Boolean.TRUE.equals(c.externalOnly()))
                p.add(
                        cb.equal(
                                root.get("actorType"),
                                com.chalchitraghar.modules.audit.enums.AuditActorType.EXTERNAL));
            if (Boolean.TRUE.equals(c.deniedOnly()))
                p.add(
                        cb.equal(
                                root.get("result"),
                                com.chalchitraghar.modules.audit.enums.AuditResult.DENIED));
            if (Boolean.TRUE.equals(c.highRiskOnly()))
                p.add(
                        root.get("severity")
                                .in(
                                        com.chalchitraghar.modules.audit.enums.AuditSeverity.HIGH,
                                        com.chalchitraghar.modules.audit.enums.AuditSeverity
                                                .CRITICAL));
            return cb.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static void present(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            List<jakarta.persistence.criteria.Predicate> p,
            jakarta.persistence.criteria.Path<String> path,
            Boolean expected) {
        if (expected == null) return;
        p.add(expected ? cb.isNotNull(path) : cb.isNull(path));
    }

    private static void equalText(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            List<jakarta.persistence.criteria.Predicate> p,
            jakarta.persistence.criteria.Path<String> path,
            String value) {
        if (value != null && !value.isBlank())
            p.add(cb.equal(cb.lower(path), value.trim().toLowerCase()));
    }

    private static void contains(
            jakarta.persistence.criteria.CriteriaBuilder cb,
            List<jakarta.persistence.criteria.Predicate> p,
            jakarta.persistence.criteria.Path<String> path,
            String value) {
        if (value != null && !value.isBlank())
            p.add(cb.like(cb.lower(path), "%" + escape(value.trim().toLowerCase()) + "%", '\\'));
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
