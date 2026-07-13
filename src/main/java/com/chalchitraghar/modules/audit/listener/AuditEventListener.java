package com.chalchitraghar.modules.audit.listener;

import com.chalchitraghar.modules.audit.dto.request.CreateAuditLogCommand;
import com.chalchitraghar.modules.audit.event.AuditEvent;
import com.chalchitraghar.modules.audit.repository.AuditLogRepository;
import com.chalchitraghar.modules.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
@RequiredArgsConstructor
public class AuditEventListener {
    private static final Logger log = LoggerFactory.getLogger(AuditEventListener.class);
    private final AuditLogService service;
    private final AuditLogRepository repository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AuditEvent event) {
        persist(event);
    }

    public void persist(AuditEvent event) {
        if (event.eventId() != null && repository.existsByEventId(event.eventId())) return;
        try {
            var actor = event.actor();
            var context = event.requestContext();
            service.append(
                    new CreateAuditLogCommand(
                            event.eventId(),
                            event.occurredAt(),
                            actor.userId(),
                            actor.emailSnapshot(),
                            actor.roleSnapshot(),
                            actor.type(),
                            event.action(),
                            event.category(),
                            event.severity(),
                            event.resourceType(),
                            event.resourceId(),
                            event.resourceReference(),
                            event.result(),
                            event.failureReason(),
                            context == null ? null : context.requestId(),
                            context == null ? null : context.correlationId(),
                            context == null ? null : context.ipAddress(),
                            context == null ? null : context.userAgent(),
                            context == null ? null : context.httpMethod(),
                            context == null ? null : context.requestPath(),
                            event.beforeValues(),
                            event.afterValues(),
                            event.metadata()));
        } catch (DataIntegrityViolationException duplicate) {
            log.debug(
                    "Duplicate audit event ignored eventId={} action={}",
                    event.eventId(),
                    event.action());
        } catch (RuntimeException failure) {
            log.error(
                    "Audit append failed eventId={} action={} actorType={} resourceType={} resourceId={} result={} exception={}",
                    event.eventId(),
                    event.action(),
                    event.actor().type(),
                    event.resourceType(),
                    event.resourceId(),
                    event.result(),
                    failure.getClass().getSimpleName());
        }
    }
}
