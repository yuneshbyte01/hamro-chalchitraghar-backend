package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.event.AuditEvent;
import com.chalchitraghar.modules.audit.listener.AuditEventListener;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Persists selected sanitized failures independently of the failing business transaction. */
@Component
@RequiredArgsConstructor
public class AuditFailureRecorder {
    private final AuditEventListener listener;

    public void record(AuditEvent event) {
        listener.persist(event);
    }
}
