package com.chalchitraghar.modules.tickets.dto.response;
import java.time.LocalDateTime;
import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
public record StaffTicketDetailResponse(String ticketReference, String bookingReference, Long customerId,
        String customerName, String customerEmail, String seatCode, SeatType seatType, String movieName,
        String hallName, LocalDateTime showDateTime, TicketStatus status, Integer qrVersion, LocalDateTime issuedAt,
        LocalDateTime checkedInAt, LocalDateTime revokedAt, LocalDateTime expiredAt, String revocationReason,
        LocalDateTime createdAt, LocalDateTime updatedAt) {}
