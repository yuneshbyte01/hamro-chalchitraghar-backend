package com.chalchitraghar.modules.bookings.service;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.bookings.repository.*;
import com.chalchitraghar.modules.notifications.event.BookingCancelledEvent;
import com.chalchitraghar.modules.payments.dto.request.CreateRefundIntentCommand;
import com.chalchitraghar.modules.payments.dto.response.CustomerBookingCancellationRefundResponse;
import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.service.*;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.tickets.repository.TicketRepository;
import com.chalchitraghar.modules.tickets.service.TicketOperationsService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.*;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BookingRefundCancellationService {
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final SeatRepository seats;
    private final TicketRepository tickets;
    private final TicketOperationsService ticketOperations;
    private final SuccessfulPaymentResolver successfulPayments;
    private final RefundService refunds;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    @Value("${app.bookings.cancellation-cutoff-minutes:0}")
    private long cancellationCutoffMinutes;

    @Transactional
    public CustomerBookingCancellationRefundResponse requestCustomerCancellation(
            String bookingReference, User user) {
        Booking visible =
                bookings.findByBookingReference(normalize(bookingReference))
                        .filter(b -> b.getUser().getId().equals(user.getId()))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Booking not found with reference: "
                                                        + bookingReference));
        if (visible.getStatus() == BookingStatus.CANCELLED) {
            return existingCustomerResult(visible, user);
        }

        var payment = successfulPayments.resolveForUpdate(visible.getId());
        Booking booking = lockBooking(visible.getId());
        if (!booking.getUser().getId().equals(user.getId()))
            throw new ResourceNotFoundException(
                    "Booking not found with reference: " + bookingReference);
        if (booking.getStatus() == BookingStatus.CANCELLED)
            return existingCustomerResult(booking, user);
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new InvalidBookingStateException(
                    "Only confirmed paid bookings can request a refund cancellation");
        validateCancellationTime(booking.getShow());
        assertNoCheckedInTicket(booking.getId());

        Refund refund =
                refunds.createRefundIntent(
                        new CreateRefundIntentCommand(
                                payment.getPaymentReference(),
                                RefundReason.CUSTOMER_CANCELLATION,
                                "CUSTOMER_CANCELLATION:" + booking.getId(),
                                user.getId()));
        cancelBooking(booking, "CUSTOMER_REFUND_REQUEST", user, true);
        return response(booking, refund);
    }

    @Transactional
    public Refund cancelConfirmedForShow(Booking candidate, Show show, User actor) {
        var payment = successfulPayments.resolveForUpdate(candidate.getId());
        Booking booking = lockBooking(candidate.getId());
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return refunds.createRefundIntent(
                    new CreateRefundIntentCommand(
                            payment.getPaymentReference(),
                            RefundReason.SHOW_CANCELLATION,
                            "SHOW_CANCELLATION:" + booking.getId() + ":" + show.getId(),
                            actor == null ? null : actor.getId()));
        }
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            throw new InvalidBookingStateException(
                    "Show cancellation found an unsupported active booking state");
        assertNoCheckedInTicket(booking.getId());
        Refund refund =
                refunds.createRefundIntent(
                        new CreateRefundIntentCommand(
                                payment.getPaymentReference(),
                                RefundReason.SHOW_CANCELLATION,
                                "SHOW_CANCELLATION:" + booking.getId() + ":" + show.getId(),
                                actor == null ? null : actor.getId()));
        refunds.approveRefund(refund.getRefundReference(), actor);
        cancelBooking(booking, "SHOW_CANCELLED_REFUND", actor, true);
        return refund;
    }

    public void assertNoCheckedInTicket(Long bookingId) {
        if (tickets.existsByBookingIdAndStatus(bookingId, TicketStatus.CHECKED_IN))
            throw new InvalidBookingStateException(
                    "Checked-in tickets require manual review and block automatic refund cancellation");
    }

    private void cancelBooking(Booking booking, String reason, User actor, boolean releaseBooked) {
        ticketOperations.revokeForBooking(booking.getId(), reason, actor);
        var claims = bookingSeats.findByBookingId(booking.getId());
        var ids = claims.stream().map(bs -> bs.getSeat().getId()).sorted().toList();
        var locked = seats.findByShowIdAndSeatIdsWithLock(booking.getShow().getId(), ids);
        if (locked.size() != ids.size())
            throw new InvalidBookingStateException("Booking seats changed during cancellation");
        if (releaseBooked) {
            locked.stream()
                    .filter(
                            s ->
                                    s.getSeatStatus() == SeatStatus.BOOKED
                                            || s.getSeatStatus() == SeatStatus.RESERVED)
                    .forEach(
                            s -> {
                                s.setSeatStatus(SeatStatus.AVAILABLE);
                                s.setLockedAt(null);
                                s.setLockExpiresAt(null);
                                s.setLockedByUserId(null);
                            });
            seats.saveAll(locked);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(now);
        bookings.save(booking);
        events.publishEvent(
                new BookingCancelledEvent(
                        booking.getUser().getId(),
                        booking.getId(),
                        booking.getBookingReference(),
                        booking.getShow().getMovie().getTitle(),
                        now));
    }

    private CustomerBookingCancellationRefundResponse existingCustomerResult(
            Booking booking, User user) {
        return refunds.getCustomerBookingRefunds(booking.getBookingReference(), user).stream()
                .filter(r -> r.reason() == RefundReason.CUSTOMER_CANCELLATION)
                .findFirst()
                .map(
                        r ->
                                new CustomerBookingCancellationRefundResponse(
                                        booking.getBookingReference(),
                                        booking.getStatus(),
                                        r.refundReference(),
                                        r.status(),
                                        r.amount(),
                                        r.currency(),
                                        r.requestedAt(),
                                        "Cancellation is recorded; the refund has not been completed."))
                .orElseThrow(
                        () ->
                                new InvalidBookingStateException(
                                        "Cancelled booking has no customer refund request"));
    }

    private CustomerBookingCancellationRefundResponse response(Booking booking, Refund refund) {
        return new CustomerBookingCancellationRefundResponse(
                booking.getBookingReference(),
                booking.getStatus(),
                refund.getRefundReference(),
                refund.getStatus(),
                refund.getAmount(),
                refund.getCurrency(),
                refund.getRequestedAt(),
                "Your refund request is recorded and awaiting review; no money has been returned yet.");
    }

    private void validateCancellationTime(Show show) {
        LocalDateTime showAt = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        if (!LocalDateTime.now(clock).isBefore(showAt.minusMinutes(cancellationCutoffMinutes)))
            throw new InvalidBookingStateException(
                    "Cancellation cutoff has passed or the show has started");
    }

    private Booking lockBooking(Long id) {
        return bookings.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", id));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
