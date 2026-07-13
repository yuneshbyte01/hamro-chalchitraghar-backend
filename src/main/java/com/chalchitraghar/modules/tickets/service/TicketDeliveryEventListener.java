package com.chalchitraghar.modules.tickets.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.*;

@Component
@RequiredArgsConstructor
public class TicketDeliveryEventListener {
    private final TicketDeliveryService service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void issued(TicketsIssuedEvent e) {
        service.queueAndAttempt(e.bookingId());
    }
}
