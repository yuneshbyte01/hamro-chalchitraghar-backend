package com.chalchitraghar.modules.bookings.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.bookings.repository.BookingSeatRepository;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

/** Centralized INITIATED booking expiry reconciliation. */
@Service
@RequiredArgsConstructor
public class BookingLifecycleService {
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final SeatRepository seatRepository;
    private final Clock clock;

    public boolean reconcileExpiry(Booking booking) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (booking.getStatus() != BookingStatus.INITIATED || booking.getExpiresAt() == null
                || now.isBefore(booking.getExpiresAt())) return false;
        var bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
        var seatIds = bookingSeats.stream().map(bs -> bs.getSeat().getId()).toList();
        var seats = seatIds.isEmpty() ? java.util.List.<com.chalchitraghar.modules.seats.entity.Seat>of()
                : seatRepository.findByShowIdAndSeatIdsWithLock(booking.getShow().getId(), seatIds);
        var activeClaims = seatIds.isEmpty() ? java.util.List.<com.chalchitraghar.modules.bookings.entity.BookingSeat>of()
                : bookingSeatRepository.findActiveBookingsBySeatIds(seatIds);
        var claimedByOtherBooking = activeClaims.stream()
                .filter(bs -> !bs.getBooking().getId().equals(booking.getId()))
                .map(bs -> bs.getSeat().getId()).collect(java.util.stream.Collectors.toSet());
        seats.stream().filter(seat -> seat.getSeatStatus() == SeatStatus.RESERVED
                && !claimedByOtherBooking.contains(seat.getId())).forEach(seat -> {
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
            seat.setLockedByUserId(null);
        });
        seatRepository.saveAll(seats);
        booking.setStatus(BookingStatus.EXPIRED);
        booking.setExpiredAt(now);
        bookingRepository.save(booking);
        return true;
    }
}
