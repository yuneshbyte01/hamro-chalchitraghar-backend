package com.chalchitraghar.modules.bookings.service.impl;

import com.chalchitraghar.modules.bookings.service.BookingService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.bookings.dto.request.BookingRequest;
import com.chalchitraghar.modules.bookings.dto.response.BookingResponse;
import com.chalchitraghar.shared.exception.InvalidBookingStateException;
import com.chalchitraghar.shared.exception.InvalidSeatSelectionException;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.exception.SeatAlreadyBookedException;
import com.chalchitraghar.shared.exception.SeatLockedException;
import com.chalchitraghar.modules.bookings.mapper.BookingMapper;
import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.entity.BookingSeat;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.bookings.repository.BookingSeatRepository;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.shared.exception.ShowConflictException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public BookingResponse createBooking(BookingRequest request, User user) {
        if (!request.hasNoDuplicateSeats()) {
            throw new InvalidSeatSelectionException("Seat IDs contain duplicates");
        }
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", request.getShowId()));
        if (show.getStatus() == ShowStatus.CANCELLED || show.getStatus() == ShowStatus.COMPLETED) {
            throw new ShowConflictException("Booking is not allowed for a cancelled or completed show");
        }
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(
                request.getShowId(), request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            List<Long> foundSeatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
            List<Long> missingSeatIds = request.getSeatIds().stream()
                    .filter(id -> !foundSeatIds.contains(id)).collect(Collectors.toList());
            throw new ResourceNotFoundException(String.format("Seats not found: %s", missingSeatIds));
        }
        List<BookingSeat> existingBookings = bookingSeatRepository.findActiveBookingsBySeatIds(request.getSeatIds());
        if (!existingBookings.isEmpty()) {
            List<Long> bookedSeatIds = existingBookings.stream()
                    .map(bs -> bs.getSeat().getId())
                    .distinct()
                    .collect(Collectors.toList());
            throw new SeatAlreadyBookedException(String.format("Seats %s are already part of an active booking", bookedSeatIds));
        }
        validateSeatsForBookingCreation(seats, user.getId());
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.INITIATED)
                .build();
        booking = bookingRepository.save(booking);
        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (Seat seat : seats) {
            bookingSeats.add(BookingSeat.builder().booking(booking).seat(seat).build());
        }
        bookingSeatRepository.saveAll(bookingSeats);
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.RESERVED);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
            seat.setLockedByUserId(null);
        }
        seatRepository.saveAll(seats);
        return bookingMapper.toResponseDto(booking, seats);
    }

    @Override
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    String.format("Booking %d does not belong to user %d", bookingId, user.getId()));
        }
        if (booking.getStatus() != BookingStatus.INITIATED) {
            throw new InvalidBookingStateException(
                    bookingId, booking.getStatus().name(), BookingStatus.INITIATED.name());
        }
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        if (bookingSeats.isEmpty()) {
            throw new ResourceNotFoundException(String.format("No seats found for booking %d", bookingId));
        }
        List<Long> seatIds = bookingSeats.stream().map(bs -> bs.getSeat().getId()).collect(Collectors.toList());
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(booking.getShow().getId(), seatIds);
        if (seats.size() != seatIds.size()) {
            List<Long> foundSeatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
            List<Long> missingSeatIds = seatIds.stream().filter(id -> !foundSeatIds.contains(id)).collect(Collectors.toList());
            throw new ResourceNotFoundException(String.format("Seats not found: %s", missingSeatIds));
        }
        List<BookingSeat> existingConfirmedBookings = bookingSeatRepository
                .findConfirmedBookingsBySeatIds(seatIds, bookingId);
        if (!existingConfirmedBookings.isEmpty()) {
            List<Long> bookedSeatIds = existingConfirmedBookings.stream()
                    .map(bs -> bs.getSeat().getId()).distinct().collect(Collectors.toList());
            throw new SeatAlreadyBookedException(
                    String.format("Seats %s are already confirmed in another booking", bookedSeatIds));
        }
        List<Seat> invalidSeats = seats.stream()
                .filter(seat -> seat.getSeatStatus() != SeatStatus.RESERVED)
                .collect(Collectors.toList());
        if (!invalidSeats.isEmpty()) {
            List<Long> invalidSeatIds = invalidSeats.stream().map(Seat::getId).collect(Collectors.toList());
            throw new SeatAlreadyBookedException(
                    String.format("Seats %s are no longer reserved for confirmation", invalidSeatIds));
        }
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.BOOKED);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
            seat.setLockedByUserId(null);
        }
        seatRepository.saveAll(seats);
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingRepository.save(booking);
        return bookingMapper.toResponseDto(booking, seats);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(User user) {
        return bookingRepository.findByUserIdOrderByBookingTimeDesc(user.getId()).stream()
                .map(booking -> {
                    List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
                    List<Seat> seats = bookingSeats.stream().map(BookingSeat::getSeat).collect(Collectors.toList());
                    return bookingMapper.toResponseDto(booking, seats);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    String.format("Booking %d does not belong to user %d", bookingId, user.getId()));
        }
        if (booking.getStatus() != BookingStatus.INITIATED) {
            throw new InvalidBookingStateException(
                    String.format("Booking %d cannot be cancelled. Current status: %s. Only INITIATED bookings can be cancelled.",
                            bookingId, booking.getStatus().name()));
        }
        Show show = booking.getShow();
        LocalDateTime showDateTime = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        if (LocalDateTime.now().isAfter(showDateTime) || LocalDateTime.now().isEqual(showDateTime)) {
            throw new InvalidBookingStateException(
                    String.format("Booking %d cannot be cancelled. Show time has passed or is in progress. Show time: %s",
                            bookingId, showDateTime));
        }
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        if (bookingSeats.isEmpty()) {
            throw new ResourceNotFoundException(String.format("No seats found for booking %d", bookingId));
        }
        List<Long> seatIds = bookingSeats.stream().map(bs -> bs.getSeat().getId()).collect(Collectors.toList());
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(booking.getShow().getId(), seatIds);
        if (seats.size() != seatIds.size()) {
            List<Long> foundSeatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
            List<Long> missingSeatIds = seatIds.stream().filter(id -> !foundSeatIds.contains(id)).collect(Collectors.toList());
            throw new ResourceNotFoundException(String.format("Seats not found: %s", missingSeatIds));
        }
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
            seat.setLockedByUserId(null);
        }
        seatRepository.saveAll(seats);
        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        return bookingMapper.toResponseDto(booking, seats);
    }

    @Override
    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (user.getRole() != Role.ADMIN && user.getRole() != Role.STAFF) {
            if (!booking.getUser().getId().equals(user.getId())) {
                throw new AccessDeniedException("You do not have access to this booking");
            }
        }
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        List<Seat> seats = bookingSeats.stream().map(BookingSeat::getSeat).collect(Collectors.toList());
        return bookingMapper.toResponseDto(booking, seats);
    }

    private void validateSeatsForBookingCreation(List<Seat> seats, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        for (Seat seat : seats) {
            if (seat.getSeatStatus() == SeatStatus.LOCKED) {
                if (isLockExpired(seat, now)) {
                    clearLock(seat);
                } else if (!userId.equals(seat.getLockedByUserId())) {
                    throw new SeatLockedException(
                            String.format("Seat %s (%s) is currently held by another user", seat.getSeatCode(), seat.getId()));
                }
            }
            if (seat.getSeatStatus() == SeatStatus.BOOKED) {
                throw new SeatAlreadyBookedException(
                        String.format("Seat %s (%s) is already booked", seat.getSeatCode(), seat.getId()));
            }
            if (seat.getSeatStatus() == SeatStatus.RESERVED) {
                throw new SeatAlreadyBookedException(
                        String.format("Seat %s (%s) is already reserved", seat.getSeatCode(), seat.getId()));
            }
            if (seat.getSeatStatus() != SeatStatus.AVAILABLE && seat.getSeatStatus() != SeatStatus.LOCKED) {
                throw new InvalidSeatSelectionException(
                        String.format("Seat %s (%s) is not available for booking. Current status: %s",
                                seat.getSeatCode(), seat.getId(), seat.getSeatStatus()));
            }
        }
    }

    private boolean isLockExpired(Seat seat, LocalDateTime now) {
        return seat.getLockExpiresAt() == null || !seat.getLockExpiresAt().isAfter(now);
    }

    private void clearLock(Seat seat) {
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seat.setLockedAt(null);
        seat.setLockExpiresAt(null);
        seat.setLockedByUserId(null);
    }
}
