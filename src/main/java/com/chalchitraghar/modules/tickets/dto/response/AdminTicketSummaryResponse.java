package com.chalchitraghar.modules.tickets.dto.response;
import java.time.LocalDateTime;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
public record AdminTicketSummaryResponse(String ticketReference, String bookingReference, String customerName,
        String seatCode, String movieName, String hallName, LocalDateTime showDateTime, TicketStatus status,
        Integer qrVersion, LocalDateTime issuedAt) {}
