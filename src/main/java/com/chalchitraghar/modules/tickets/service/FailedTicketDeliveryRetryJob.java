package com.chalchitraghar.modules.tickets.service;

import com.chalchitraghar.shared.observability.JobName;
import com.chalchitraghar.shared.observability.ScheduledJobObserver;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FailedTicketDeliveryRetryJob {
    private final TicketDeliveryService service;
    private final ScheduledJobObserver jobs;

    @Scheduled(fixedDelayString = "${app.tickets.email.retry-interval-ms:60000}")
    public int run() {
        return jobs.observe(JobName.TICKET_DELIVERY_RETRY, service::retryBatch);
    }
}
