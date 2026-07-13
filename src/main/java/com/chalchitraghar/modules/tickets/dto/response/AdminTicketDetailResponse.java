package com.chalchitraghar.modules.tickets.dto.response;

import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;

public record AdminTicketDetailResponse(
        String ticketReference,
        String bookingReference,
        Long customerId,
        String customerName,
        String customerEmail,
        String seatCode,
        SeatType seatType,
        String movieName,
        String hallName,
        LocalDateTime showDateTime,
        TicketStatus status,
        Integer qrVersion,
        LocalDateTime issuedAt,
        LocalDateTime checkedInAt,
        LocalDateTime revokedAt,
        LocalDateTime expiredAt,
        String revocationReason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<TicketValidationHistoryResponse> validationHistory) {}
