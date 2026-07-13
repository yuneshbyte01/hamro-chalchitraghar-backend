package com.chalchitraghar.modules.audit.service;

import com.chalchitraghar.modules.audit.event.AuditEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditEventPublisher {
    private final ApplicationEventPublisher publisher;

    public void publish(AuditEvent event) {
        publisher.publishEvent(event);
    }
}
