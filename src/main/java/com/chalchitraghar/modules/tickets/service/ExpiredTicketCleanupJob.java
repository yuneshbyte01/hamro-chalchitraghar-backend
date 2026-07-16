package com.chalchitraghar.modules.tickets.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExpiredTicketCleanupJob {
    private final TicketOperationsService service;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.tickets.expiry-reconciliation-interval-ms:60000}")
    public int run() {
        return jobs.observe(JobName.TICKET_EXPIRY, service::expireBatch);
    }
}
