package com.chalchitraghar.modules.bookings.service;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.bookings.repository.BookingSeatRepository;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Authoritative locked expiry and initiated-cancellation processing. */
@Service
@RequiredArgsConstructor
public class BookingLifecycleService {
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final Clock clock;

    @Transactional
    public boolean reconcileExpiry(Booking booking) {
        return expireBooking(booking.getId(), false);
    }

    @Transactional
    public boolean expireBooking(Long bookingId, boolean requireExpired) {
        Booking booking =
                bookingRepository
                        .findByIdForUpdate(bookingId)
                        .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (booking.getStatus() == BookingStatus.EXPIRED) return false;
        if (booking.getStatus() != BookingStatus.INITIATED) return false;
        LocalDateTime now = LocalDateTime.now(clock);
        boolean expired = booking.getExpiresAt() != null && !now.isBefore(booking.getExpiresAt());
        if (!expired) return false;
        releaseReservedSeats(booking);
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setExpiredAt(now);
        bookingRepository.save(booking);
        return true;
    }

    @Transactional
    public int cancelInitiatedBookingsForShow(Long showId) {
        int cancelled = 0;
        for (Booking candidate :
                bookingRepository.findByShowIdAndStatus(showId, BookingStatus.INITIATED)) {
            Booking booking = bookingRepository.findByIdForUpdate(candidate.getId()).orElseThrow();
            if (booking.getStatus() != BookingStatus.INITIATED) continue;
            releaseReservedSeats(booking);
            booking.setStatus(BookingStatus.CANCELLED);
            booking.setCancelledAt(LocalDateTime.now(clock));
            bookingRepository.save(booking);
            cancelled++;
        }
        return cancelled;
    }

    private void releaseReservedSeats(Booking booking) {
        var claims = bookingSeatRepository.findByBookingId(booking.getId());
        var seatIds =
                claims.stream()
                        .peek(
                                bs -> {
                                    if (!bs.getSeat()
                                            .getShow()
                                            .getId()
                                            .equals(booking.getShow().getId())) {
                                        throw new IllegalStateException(
                                                "Booking seat belongs to a different show");
                                    }
                                })
                        .map(bs -> bs.getSeat().getId())
                        .sorted()
                        .toList();
        if (seatIds.isEmpty()) return;
        var seats =
                seatRepository
                        .findByShowIdAndSeatIdsWithLock(booking.getShow().getId(), seatIds)
                        .stream()
                        .sorted(Comparator.comparing(seat -> seat.getId()))
                        .toList();
        if (seats.size() != seatIds.size())
            throw new IllegalStateException("Booking seat state is inconsistent");
        var otherClaims =
                bookingSeatRepository.findActiveBookingsBySeatIds(seatIds).stream()
                        .filter(bs -> !bs.getBooking().getId().equals(booking.getId()))
                        .map(bs -> bs.getSeat().getId())
                        .collect(java.util.stream.Collectors.toSet());
        seats.stream()
                .filter(
                        seat ->
                                seat.getSeatStatus() == SeatStatus.RESERVED
                                        && !otherClaims.contains(seat.getId()))
                .forEach(
                        seat -> {
                            seat.setSeatStatus(SeatStatus.AVAILABLE);
                            seat.setLockedAt(null);
                            seat.setLockExpiresAt(null);
                            seat.setLockedByUserId(null);
                        });
        seatRepository.saveAll(seats);
    }
}
