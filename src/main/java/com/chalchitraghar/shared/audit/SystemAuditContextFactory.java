package com.chalchitraghar.shared.audit;

import java.time.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SystemAuditContextFactory {
    private final Clock clock;

    public RequestAuditContext create(String jobName) {
        String runId = UUID.randomUUID().toString();
        return new RequestAuditContext(
                jobName + ":" + runId,
                runId,
                null,
                null,
                null,
                null,
                jobName,
                LocalDateTime.now(clock));
    }
}
