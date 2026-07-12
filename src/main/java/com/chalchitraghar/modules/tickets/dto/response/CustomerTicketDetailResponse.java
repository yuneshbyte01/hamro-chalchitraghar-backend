package com.chalchitraghar.modules.tickets.dto.response;
import java.time.LocalDateTime;
import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
public record CustomerTicketDetailResponse(String ticketReference, String bookingReference, String seatCode,
        SeatType seatType, String movieName, String hallName, LocalDateTime showDateTime, String showStartTime,
        String showEndTime, TicketStatus status, boolean checkedIn, LocalDateTime checkedInAt, Integer qrVersion, LocalDateTime issuedAt) {}
