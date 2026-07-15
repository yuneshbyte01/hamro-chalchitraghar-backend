package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.tickets.repository.TicketRepository;
import com.chalchitraghar.shared.exception.PaymentConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundEligibilityService {
    private final TicketRepository tickets;
    private final PaymentRepository payments;

    public void validate(
            Payment payment,
            Booking booking,
            RefundReason reason,
            RefundType type,
            RefundMethod method) {
        if (payment.getStatus() != PaymentStatus.SUCCESS)
            conflict("Only successful payments are refundable");
        if (payment.getAmount() == null || payment.getAmount().signum() <= 0)
            conflict("Payment amount must be positive");
        if (payment.getCurrency() == null || payment.getCurrency().isBlank())
            conflict("Payment currency is required");
        if (payment.getBooking() == null || !payment.getBooking().getId().equals(booking.getId()))
            conflict("Payment and booking do not match");
        if (type != RefundType.FULL) conflict("Refund-1 supports full refunds only");
        if (method != RefundMethod.MANUAL) conflict("Refund-1 supports manual refund intent only");
        if (tickets.existsByBookingIdAndStatus(booking.getId(), TicketStatus.CHECKED_IN))
            conflict("Checked-in tickets require manual financial review");

        switch (reason) {
            case SHOW_CANCELLATION -> {
                if (booking.getShow().getStatus() != ShowStatus.CANCELLED)
                    conflict("Show cancellation refunds require a cancelled show");
            }
            case CUSTOMER_CANCELLATION -> {
                if (booking.getStatus() != BookingStatus.CANCELLED)
                    conflict("Customer cancellation refunds require a cancelled booking");
            }
            case LATE_PAYMENT_SUCCESS -> {
                boolean unconfirmable =
                        booking.getStatus() == BookingStatus.CANCELLED
                                || booking.getStatus() == BookingStatus.EXPIRED
                                || payment.isManualReviewRequired();
                if (!unconfirmable) conflict("Payment is not a confirmed late-success case");
            }
            case DUPLICATE_PAYMENT -> {
                if (payments.countByBookingIdAndStatus(booking.getId(), PaymentStatus.SUCCESS) < 2)
                    conflict("Duplicate payment reason requires multiple successful payments");
            }
            case ADMIN_ADJUSTMENT, OTHER -> {
                /* Trusted internal reasons. */
            }
        }
    }

    private void conflict(String message) {
        throw new PaymentConflictException(message);
    }
}
