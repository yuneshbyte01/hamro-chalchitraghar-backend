package com.chalchitraghar.modules.tickets.mapper;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.tickets.entity.Ticket;

@Component
public class TicketMapper {
    public CustomerTicketSummaryResponse toCustomerSummary(Ticket t) {
        return new CustomerTicketSummaryResponse(t.getTicketReference(), t.getBooking().getBookingReference(), seatCode(t),
                movie(t), hall(t), showDateTime(t), t.getStatus(), t.getQrTokenVersion(), t.getIssuedAt());
    }
    public CustomerTicketDetailResponse toCustomerDetail(Ticket t) {
        return new CustomerTicketDetailResponse(t.getTicketReference(), t.getBooking().getBookingReference(), seatCode(t),
                t.getBookingSeat().getSeat().getSeatType(), movie(t), hall(t), showDateTime(t),
                t.getBooking().getShow().getShowTime().toString(), t.getBooking().getShow().getEndTime().toString(),
                t.getStatus(), t.getQrTokenVersion(), t.getIssuedAt());
    }
    public StaffTicketSummaryResponse toStaffSummary(Ticket t) {
        return new StaffTicketSummaryResponse(t.getTicketReference(), t.getBooking().getBookingReference(),
                t.getBooking().getUser().getName(), seatCode(t), movie(t), hall(t), showDateTime(t), t.getStatus(), t.getQrTokenVersion(), t.getIssuedAt());
    }
    public StaffTicketDetailResponse toStaffDetail(Ticket t) {
        var u=t.getBooking().getUser();
        return new StaffTicketDetailResponse(t.getTicketReference(), t.getBooking().getBookingReference(), u.getId(),
                u.getName(), u.getEmail(), seatCode(t), t.getBookingSeat().getSeat().getSeatType(), movie(t), hall(t),
                showDateTime(t), t.getStatus(), t.getQrTokenVersion(), t.getIssuedAt(), t.getCheckedInAt(), t.getRevokedAt(), t.getExpiredAt(),
                t.getRevocationReason(), t.getCreatedAt(), t.getUpdatedAt());
    }
    public AdminTicketSummaryResponse toAdminSummary(Ticket t) {
        return new AdminTicketSummaryResponse(t.getTicketReference(), t.getBooking().getBookingReference(),
                t.getBooking().getUser().getName(), seatCode(t), movie(t), hall(t), showDateTime(t), t.getStatus(), t.getQrTokenVersion(), t.getIssuedAt());
    }
    public AdminTicketDetailResponse toAdminDetail(Ticket t) {
        var u=t.getBooking().getUser();
        return new AdminTicketDetailResponse(t.getTicketReference(), t.getBooking().getBookingReference(), u.getId(),
                u.getName(), u.getEmail(), seatCode(t), t.getBookingSeat().getSeat().getSeatType(), movie(t), hall(t),
                showDateTime(t), t.getStatus(), t.getQrTokenVersion(), t.getIssuedAt(), t.getCheckedInAt(), t.getRevokedAt(), t.getExpiredAt(),
                t.getRevocationReason(), t.getCreatedAt(), t.getUpdatedAt());
    }
    private String seatCode(Ticket t){return t.getBookingSeat().getSeat().getSeatCode();}
    private String movie(Ticket t){return t.getBooking().getShow().getMovie().getTitle();}
    private String hall(Ticket t){return t.getBooking().getShow().getHall().getName();}
    private LocalDateTime showDateTime(Ticket t){return LocalDateTime.of(t.getBooking().getShow().getShowDate(),t.getBooking().getShow().getShowTime());}
}
