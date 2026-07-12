package com.chalchitraghar.modules.tickets.dto.response;
import java.time.LocalDateTime;
import com.chalchitraghar.modules.tickets.enums.ValidationResult;
public record TicketScanResponse(ValidationResult result,String reason,boolean admitted,String ticketReference,
        String bookingReference,String seatCode,String movieName,String hallName,LocalDateTime showDateTime,
        LocalDateTime checkedInAt) {}
