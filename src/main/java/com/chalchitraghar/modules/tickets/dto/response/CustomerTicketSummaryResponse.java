package com.chalchitraghar.modules.tickets.dto.response;
import java.time.LocalDateTime;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
public record CustomerTicketSummaryResponse(String ticketReference, String bookingReference, String seatCode,
        String movieName, String hallName, LocalDateTime showDateTime, TicketStatus status, LocalDateTime issuedAt) {}
