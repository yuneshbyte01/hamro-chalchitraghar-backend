package com.chalchitraghar.modules.payments.mapper;

import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RefundMapper {
    public CustomerRefundSummaryResponse toCustomerSummary(Refund r) {
        return new CustomerRefundSummaryResponse(
                r.getRefundReference(),
                r.getBooking().getBookingReference(),
                r.getPayment().getPaymentReference(),
                r.getAmount(),
                r.getCurrency(),
                r.getStatus(),
                r.getReason(),
                r.getType(),
                r.getRequestedAt());
    }

    public CustomerRefundDetailResponse toCustomerDetail(Refund r) {
        var show = r.getBooking().getShow();
        return new CustomerRefundDetailResponse(
                r.getRefundReference(),
                r.getBooking().getBookingReference(),
                r.getPayment().getPaymentReference(),
                show.getMovie().getTitle(),
                show.getHall().getName(),
                LocalDateTime.of(show.getShowDate(), show.getShowTime()),
                r.getAmount(),
                r.getCurrency(),
                r.getStatus(),
                r.getReason(),
                r.getType(),
                r.getMethod(),
                r.getRequestedAt(),
                r.getApprovedAt(),
                r.getRejectedAt(),
                statusMessage(r));
    }

    public AdminRefundSummaryResponse toAdminSummary(Refund r) {
        var user = r.getBooking().getUser();
        return new AdminRefundSummaryResponse(
                r.getRefundReference(),
                r.getBooking().getBookingReference(),
                r.getPayment().getPaymentReference(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                r.getAmount(),
                r.getCurrency(),
                r.getStatus(),
                r.getReason(),
                r.getType(),
                r.getMethod(),
                r.getRequestedAt());
    }

    public AdminRefundDetailResponse toAdminDetail(Refund r, List<Ticket> tickets) {
        var booking = r.getBooking();
        var user = booking.getUser();
        return new AdminRefundDetailResponse(
                r.getRefundReference(),
                booking.getBookingReference(),
                r.getPayment().getPaymentReference(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                r.getAmount(),
                r.getCurrency(),
                r.getStatus(),
                r.getReason(),
                r.getType(),
                r.getMethod(),
                r.getPayment().getProvider(),
                r.getPayment().getStatus(),
                booking.getStatus(),
                booking.getShow().getStatus(),
                ticketSummary(tickets),
                actor(r.getRequestedBy()),
                actor(r.getApprovedBy()),
                actor(r.getRejectedBy()),
                r.getRequestedAt(),
                r.getApprovedAt(),
                r.getRejectedAt(),
                r.getProviderRefundReference(),
                r.getFailureReason(),
                r.getCreatedAt(),
                r.getUpdatedAt());
    }

    private TicketStatusSummary ticketSummary(List<Ticket> tickets) {
        return new TicketStatusSummary(
                count(tickets, TicketStatus.ISSUED),
                count(tickets, TicketStatus.CHECKED_IN),
                count(tickets, TicketStatus.REVOKED),
                count(tickets, TicketStatus.EXPIRED));
    }

    private long count(List<Ticket> tickets, TicketStatus status) {
        return tickets.stream().filter(t -> t.getStatus() == status).count();
    }

    private String actor(com.chalchitraghar.modules.users.entity.User user) {
        return user == null ? null : user.getName() + " <" + user.getEmail() + ">";
    }

    private String statusMessage(Refund r) {
        return switch (r.getStatus()) {
            case REQUESTED -> "Refund requested; no money has been returned yet.";
            case APPROVED -> "Refund approved and awaiting processing.";
            case REJECTED -> "Refund request rejected.";
            case PROCESSING -> "Refund processing is in progress.";
            case SUCCEEDED -> "Refund completed.";
            case FAILED -> "Refund processing failed; contact support if assistance is needed.";
            case MANUAL_REVIEW -> "Refund requires manual review.";
        };
    }
}
