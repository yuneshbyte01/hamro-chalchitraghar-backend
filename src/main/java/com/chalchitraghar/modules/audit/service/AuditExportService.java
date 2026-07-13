package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.config.AuditOperationsProperties;
import com.chalchitraghar.modules.audit.dto.request.AdminAuditLogFilterRequest;
import com.chalchitraghar.modules.audit.entity.AuditLog;
import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.event.AuditActor;
import com.chalchitraghar.modules.audit.event.AuditEvent;
import com.chalchitraghar.modules.audit.listener.AuditEventListener;
import com.chalchitraghar.modules.audit.repository.AuditLogRepository;
import com.chalchitraghar.modules.audit.specification.AuditLogSpecification;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditExportService {
    private static final String HEADER =
            "auditId,occurredAt,actorType,actorUserId,actorEmail,actorRole,action,category,severity,result,resourceType,resourceId,resourceReference,requestId,correlationId,httpMethod,requestPath,failureReason,createdAt\r\n";
    private final AuditLogRepository repository;
    private final AuditOperationsProperties properties;
    private final AuditEventListener events;
    private final Clock clock;

    @Transactional(readOnly = true)
    public int writeCsv(AdminAuditLogFilterRequest filter, AuditActor actor, OutputStream output)
            throws IOException {
        var spec = AuditLogSpecification.matching(filter);
        Writer writer = new BufferedWriter(new OutputStreamWriter(output, StandardCharsets.UTF_8));
        writer.write('\ufeff');
        writer.write(HEADER);
        int page = 0;
        int written = 0;
        Page<AuditLog> rows;
        do {
            rows =
                    repository.findAll(
                            spec,
                            PageRequest.of(
                                    page++,
                                    Math.min(500, properties.getExport().getMaxRows()),
                                    Sort.by(Sort.Order.desc("occurredAt"), Sort.Order.desc("id"))));
            for (AuditLog a : rows) {
                writeRow(writer, a);
                written++;
            }
        } while (rows.hasNext());
        writer.flush();
        auditSuccess(filter, actor, written);
        return written;
    }

    @Transactional(readOnly = true)
    public long preflight(AdminAuditLogFilterRequest filter) {
        validate(filter);
        long count = repository.count(AuditLogSpecification.matching(filter));
        if (count > properties.getExport().getMaxRows())
            throw new IllegalArgumentException("Export exceeds maximum row count");
        return count;
    }

    private void validate(AdminAuditLogFilterRequest f) {
        if (!properties.getExport().isEnabled())
            throw new IllegalArgumentException("Audit export is disabled");
        if (f.occurredFrom() == null || f.occurredTo() == null)
            throw new IllegalArgumentException("Export requires occurredFrom and occurredTo");
        if (f.occurredFrom().isAfter(f.occurredTo()))
            throw new IllegalArgumentException("Invalid export date range");
        if (Duration.between(f.occurredFrom(), f.occurredTo())
                        .compareTo(Duration.ofDays(properties.getExport().getMaxRangeDays()))
                > 0) throw new IllegalArgumentException("Export date range is too large");
    }

    private void writeRow(Writer w, AuditLog a) throws IOException {
        Object[] values = {
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
            a.getResourceId(),
            a.getResourceReference(),
            a.getRequestId(),
            a.getCorrelationId(),
            a.getHttpMethod(),
            a.getRequestPath(),
            a.getFailureReason(),
            a.getCreatedAt()
        };
        for (int i = 0; i < values.length; i++) {
            if (i > 0) w.write(',');
            w.write(csv(values[i]));
        }
        w.write("\r\n");
    }

    static String csv(Object raw) {
        if (raw == null) return "";
        String value = raw.toString().replaceAll("[\\r\\n\\t]+", " ");
        String visible = value.stripLeading();
        if (!visible.isEmpty() && "=+-@".indexOf(visible.charAt(0)) >= 0) value = "'" + value;
        return '"' + value.replace("\"", "\"\"") + '"';
    }

    private String mask(String email) {
        if (email == null) return null;
        int at = email.indexOf('@');
        return at < 0 ? "***" : email.substring(0, Math.min(2, at)) + "***" + email.substring(at);
    }

    private void auditSuccess(AdminAuditLogFilterRequest f, AuditActor actor, int count) {
        events.persist(
                new AuditEvent(
                        "AUDIT_EXPORTED:" + java.util.UUID.randomUUID(),
                        LocalDateTime.now(clock),
                        actor,
                        AuditAction.AUDIT_EXPORTED,
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
                                "format",
                                "CSV",
                                "count",
                                count,
                                "exportFrom",
                                f.occurredFrom().toString(),
                                "exportTo",
                                f.occurredTo().toString(),
                                "filterSummary",
                                "approved filters")));
    }
}
