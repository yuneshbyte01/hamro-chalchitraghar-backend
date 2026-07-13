package com.chalchitraghar.modules.tickets.service;

import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import org.springframework.stereotype.Service;

@Service
public class TicketLifecycleService {
    public void transition(Ticket ticket, TicketStatus target) {
        if (ticket.getStatus() != TicketStatus.ISSUED
                || !(target == TicketStatus.CHECKED_IN
                        || target == TicketStatus.REVOKED
                        || target == TicketStatus.EXPIRED))
            throw new IllegalStateException(
                    "Invalid ticket status transition from "
                            + ticket.getStatus()
                            + " to "
                            + target);
        ticket.setStatus(target);
    }
}
