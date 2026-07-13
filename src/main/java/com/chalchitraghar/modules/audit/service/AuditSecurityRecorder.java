package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.factory.*;
import com.chalchitraghar.shared.audit.RequestAuditContextHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditSecurityRecorder {
    private final AuditEventFactory events;
    private final AuditActorResolver actors;
    private final AuditFailureRecorder failures;

    public void invalidJwt(String code) {
        if (!RequestAuditContextHolder.markSecurityAudit("INVALID_JWT")) return;
        failures.record(
                events.attempt(
                        actors.anonymous(null),
                        AuditAction.INVALID_JWT_REJECTED,
                        AuditCategory.SECURITY,
                        AuditSeverity.WARNING,
                        AuditResult.DENIED,
                        "SECURITY",
                        null,
                        null,
                        code,
                        java.util.Map.of("reason", code)));
    }

    public void accessDenied() {
        if (!RequestAuditContextHolder.markSecurityAudit("ACCESS_DENIED")) return;
        var context = RequestAuditContextHolder.current().orElse(null);
        String path = context == null ? null : context.requestPath();
        failures.record(
                events.attempt(
                        actors.currentUserOrSystem(),
                        AuditAction.ACCESS_DENIED,
                        AuditCategory.SECURITY,
                        AuditSeverity.WARNING,
                        AuditResult.DENIED,
                        "SECURITY",
                        null,
                        path,
                        "ACCESS_DENIED",
                        null));
    }
}
