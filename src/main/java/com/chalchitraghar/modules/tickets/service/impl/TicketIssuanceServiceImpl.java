package com.chalchitraghar.modules.tickets.service.impl;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.bookings.repository.BookingSeatRepository;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import com.chalchitraghar.modules.tickets.repository.TicketRepository;
import com.chalchitraghar.modules.tickets.service.TicketIssuanceService;
import com.chalchitraghar.modules.tickets.service.TicketReferenceGenerator;
import com.chalchitraghar.shared.exception.InvalidBookingStateException;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TicketIssuanceServiceImpl implements TicketIssuanceService {
    private final TicketRepository tickets;
    private final BookingRepository bookings;
    private final BookingSeatRepository bookingSeats;
    private final TicketReferenceGenerator references;
    private final Clock clock;
    private final com.chalchitraghar.modules.tickets.service.QrTokenService qrTokens;

    @Override
    @Transactional
    public List<Ticket> issueTicketsForConfirmedBooking(Booking supplied) {
        if (supplied == null || supplied.getId() == null) throw new ResourceNotFoundException("Booking not found");
        Booking booking = bookings.findByIdForUpdate(supplied.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", supplied.getId()));
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new InvalidBookingStateException("Tickets can only be issued for a CONFIRMED booking");
        }
        var claims = bookingSeats.findByBookingId(booking.getId()).stream()
                .sorted(Comparator.comparing(bs -> bs.getSeat().getPositionIndex())).toList();
        if (claims.isEmpty()) throw new IllegalStateException("Confirmed booking has no seats");
        if (claims.stream().anyMatch(bs -> bs.getSeat().getSeatStatus() != SeatStatus.BOOKED)) {
            throw new IllegalStateException("Every ticket requires a BOOKED seat");
        }
        LocalDateTime issuedAt = LocalDateTime.now(clock);
        List<Ticket> result = new ArrayList<>();
        for (var claim : claims) {
            Ticket existing = tickets.findByBookingSeatId(claim.getId()).orElse(null);
            if (existing != null) { result.add(existing); continue; }
            String reference;
            do reference = references.generate(); while (tickets.existsByTicketReference(reference));
            com.chalchitraghar.modules.tickets.service.QrTokenService.PreparedQrToken qr;
            do qr = qrTokens.prepareToken(); while (tickets.existsByQrTokenHash(qr.tokenHash()));
            result.add(tickets.save(Ticket.builder().booking(booking).bookingSeat(claim).ticketReference(reference)
                    .status(TicketStatus.ISSUED).issuedAt(issuedAt).qrTokenEncrypted(qr.encryptedToken())
                    .qrTokenHash(qr.tokenHash()).qrTokenVersion(qr.version()).qrKeyId(qr.keyId()).qrIssuedAt(issuedAt).build()));
        }
        tickets.flush();
        return result;
    }

    @Override
    @Transactional
    public List<Ticket> backfillConfirmedBooking(Long bookingId) {
        Booking booking = bookings.findById(bookingId).orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        return issueTicketsForConfirmedBooking(booking);
    }
}
