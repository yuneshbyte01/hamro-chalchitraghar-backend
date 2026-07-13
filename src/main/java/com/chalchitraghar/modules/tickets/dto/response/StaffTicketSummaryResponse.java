package com.chalchitraghar.modules.tickets.dto.response;

import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import java.time.LocalDateTime;

public record StaffTicketSummaryResponse(
        String ticketReference,
        String bookingReference,
        String customerName,
        String seatCode,
        String movieName,
        String hallName,
        LocalDateTime showDateTime,
        TicketStatus status,
        Integer qrVersion,
        LocalDateTime issuedAt) {}
