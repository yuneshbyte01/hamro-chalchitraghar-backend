package com.chalchitraghar.modules.tickets.dto.response;

import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import java.time.LocalDateTime;

public record CustomerTicketSummaryResponse(
        String ticketReference,
        String bookingReference,
        String seatCode,
        String movieName,
        String hallName,
        LocalDateTime showDateTime,
        TicketStatus status,
        Integer qrVersion,
        LocalDateTime issuedAt) {}
