package com.chalchitraghar.modules.tickets.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FailedTicketDeliveryRetryJob {
    private final TicketDeliveryService service;

    @Scheduled(fixedDelayString = "${app.tickets.email.retry-interval-ms:60000}")
    public int run() {
        return service.retryBatch();
    }
}
