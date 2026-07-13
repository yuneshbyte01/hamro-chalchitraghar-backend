package com.chalchitraghar.modules.tickets.service.impl;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.tickets.config.TicketQrProperties;
import com.chalchitraghar.modules.tickets.dto.request.TicketScanRequest;
import com.chalchitraghar.modules.tickets.dto.response.TicketScanResponse;
import com.chalchitraghar.modules.tickets.entity.*;
import com.chalchitraghar.modules.tickets.enums.*;
import com.chalchitraghar.modules.tickets.repository.*;
import com.chalchitraghar.modules.tickets.service.*;
import com.chalchitraghar.modules.users.entity.User;
import java.time.Clock;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketValidationServiceImpl implements TicketValidationService {
    private final TicketRepository tickets;
    private final TicketValidationRepository validations;
    private final QrTokenService qrTokens;
    private final TicketLifecycleService lifecycle;
    private final TicketQrProperties properties;
    private final Clock clock;

    @Transactional
    public TicketScanResponse scan(
            TicketScanRequest request,
            User staff,
            String device,
            String location,
            String requestId) {
        LocalDateTime now = LocalDateTime.now(clock);
        String hash = qrTokens.hashToken(request.qrToken());
        Ticket ticket = tickets.findByQrTokenHashForUpdate(hash).orElse(null);
        if (ticket == null) {
            save(
                    null,
                    staff,
                    now,
                    ValidationResult.INVALID,
                    "QR token is invalid",
                    device,
                    location,
                    requestId);
            return response(null, ValidationResult.INVALID, "QR token is invalid", false);
        }
        if (ticket.getStatus() == TicketStatus.CHECKED_IN)
            return reject(
                    ticket,
                    staff,
                    now,
                    ValidationResult.ALREADY_USED,
                    "Ticket has already been checked in",
                    device,
                    location,
                    requestId);
        if (ticket.getStatus() == TicketStatus.REVOKED)
            return reject(
                    ticket,
                    staff,
                    now,
                    ValidationResult.REVOKED,
                    "Ticket is revoked",
                    device,
                    location,
                    requestId);
        if (ticket.getStatus() == TicketStatus.EXPIRED)
            return reject(
                    ticket,
                    staff,
                    now,
                    ValidationResult.EXPIRED,
                    "Ticket is expired",
                    device,
                    location,
                    requestId);
        var booking = ticket.getBooking();
        if (booking.getStatus() == BookingStatus.CANCELLED
                || booking.getStatus() == BookingStatus.EXPIRED)
            return reject(
                    ticket,
                    staff,
                    now,
                    ValidationResult.BOOKING_CANCELLED,
                    "Booking is not confirmed",
                    device,
                    location,
                    requestId);
        if (booking.getStatus() != BookingStatus.CONFIRMED)
            return reject(
                    ticket,
                    staff,
                    now,
                    ValidationResult.INVALID,
                    "Booking is not confirmed",
                    device,
                    location,
                    requestId);
        var show = booking.getShow();
        if (show.getStatus() == ShowStatus.CANCELLED)
            return reject(
                    ticket,
                    staff,
                    now,
                    ValidationResult.SHOW_CANCELLED,
                    "Show is cancelled",
                    device,
                    location,
                    requestId);
        LocalDateTime start = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        LocalDateTime end = LocalDateTime.of(show.getShowDate(), show.getEndTime());
        if (now.isBefore(start.minusMinutes(properties.entryWindowMinutes())))
            return reject(
                    ticket,
                    staff,
                    now,
                    ValidationResult.TOO_EARLY,
                    "Ticket is not yet valid for entry",
                    device,
                    location,
                    requestId);
        if (now.isAfter(end.plusMinutes(properties.postShowGraceMinutes())))
            return reject(
                    ticket,
                    staff,
                    now,
                    ValidationResult.TOO_LATE,
                    "Ticket entry window has ended",
                    device,
                    location,
                    requestId);
        lifecycle.transition(ticket, TicketStatus.CHECKED_IN);
        ticket.setCheckedInAt(now);
        ticket.setCheckedInBy(staff);
        tickets.save(ticket);
        save(
                ticket,
                staff,
                now,
                ValidationResult.SUCCESS,
                "Ticket checked in successfully",
                device,
                location,
                requestId);
        return response(ticket, ValidationResult.SUCCESS, "Ticket checked in successfully", true);
    }

    private TicketScanResponse reject(
            Ticket t,
            User u,
            LocalDateTime now,
            ValidationResult r,
            String reason,
            String d,
            String l,
            String id) {
        save(t, u, now, r, reason, d, l, id);
        return response(t, r, reason, false);
    }

    private void save(
            Ticket t,
            User u,
            LocalDateTime now,
            ValidationResult r,
            String reason,
            String d,
            String l,
            String id) {
        validations.save(
                TicketValidation.builder()
                        .ticket(t)
                        .validatedBy(u)
                        .validationTime(now)
                        .result(r)
                        .reason(reason)
                        .deviceId(trim(d))
                        .location(trim(l))
                        .requestId(trim(id))
                        .build());
    }

    private String trim(String value) {
        if (value == null || value.isBlank()) return null;
        String s = value.trim();
        return s.length() > 200 ? s.substring(0, 200) : s;
    }

    private TicketScanResponse response(
            Ticket t, ValidationResult r, String reason, boolean admitted) {
        if (t == null)
            return new TicketScanResponse(
                    r, reason, false, null, null, null, null, null, null, null);
        var b = t.getBooking();
        return new TicketScanResponse(
                r,
                reason,
                admitted,
                t.getTicketReference(),
                b.getBookingReference(),
                t.getBookingSeat().getSeat().getSeatCode(),
                b.getShow().getMovie().getTitle(),
                b.getShow().getHall().getName(),
                LocalDateTime.of(b.getShow().getShowDate(), b.getShow().getShowTime()),
                t.getCheckedInAt());
    }
}
