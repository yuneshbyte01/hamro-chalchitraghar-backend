package com.chalchitraghar.modules.tickets.dto.response;

import com.chalchitraghar.modules.tickets.enums.ValidationResult;
import java.time.LocalDateTime;

public record TicketScanResponse(
        ValidationResult result,
        String reason,
        boolean admitted,
        String ticketReference,
        String bookingReference,
        String seatCode,
        String movieName,
        String hallName,
        LocalDateTime showDateTime,
        LocalDateTime checkedInAt) {}
