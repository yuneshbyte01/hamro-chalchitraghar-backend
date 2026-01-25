package com.chalchitraghar.service.booking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.dto.booking.BookingRequest;
import com.chalchitraghar.dto.booking.BookingResponse;
import com.chalchitraghar.exception.InvalidBookingStateException;
import com.chalchitraghar.exception.InvalidSeatSelectionException;
import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.exception.SeatAlreadyBookedException;
import com.chalchitraghar.mapper.BookingMapper;
import com.chalchitraghar.model.Booking;
import com.chalchitraghar.model.BookingSeat;
import com.chalchitraghar.model.Seat;
import com.chalchitraghar.model.Show;
import com.chalchitraghar.model.User;
import com.chalchitraghar.model.enums.BookingStatus;
import com.chalchitraghar.model.enums.Role;
import com.chalchitraghar.model.enums.SeatStatus;
import com.chalchitraghar.repository.BookingRepository;
import com.chalchitraghar.repository.BookingSeatRepository;
import com.chalchitraghar.repository.SeatRepository;
import com.chalchitraghar.repository.ShowRepository;

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
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(
                request.getShowId(), request.getSeatIds());
        if (seats.size() != request.getSeatIds().size()) {
            List<Long> foundSeatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
            List<Long> missingSeatIds = request.getSeatIds().stream()
                    .filter(id -> !foundSeatIds.contains(id)).collect(Collectors.toList());
            throw new ResourceNotFoundException(String.format("Seats not found: %s", missingSeatIds));
        }
        List<Seat> invalidSeats = seats.stream()
                .filter(seat -> !seat.getShow().getId().equals(request.getShowId()))
                .collect(Collectors.toList());
        if (!invalidSeats.isEmpty()) {
            throw new InvalidSeatSelectionException(
                    String.format("Seats %s do not belong to show %d",
                            invalidSeats.stream().map(Seat::getId).collect(Collectors.toList()),
                            request.getShowId()));
        }
        List<BookingSeat> existingBookings = bookingSeatRepository.findBySeatIds(request.getSeatIds());
        if (!existingBookings.isEmpty()) {
            List<Long> bookedSeatIds = existingBookings.stream()
                    .map(bs -> bs.getSeat().getId()).collect(Collectors.toList());
            throw new SeatAlreadyBookedException(String.format("Seats %s are already booked", bookedSeatIds));
        }
        List<Seat> unavailableSeats = seats.stream()
                .filter(seat -> {
                    if (seat.getSeatStatus() == SeatStatus.BOOKED) return true;
                    if (seat.getSeatStatus() == SeatStatus.RESERVED) return true;
                    if (seat.getSeatStatus() == SeatStatus.LOCKED) {
                        if (seat.getLockExpiresAt() != null && seat.getLockExpiresAt().isAfter(LocalDateTime.now()))
                            return true;
                    }
                    return false;
                })
                .collect(Collectors.toList());
        if (!unavailableSeats.isEmpty()) {
            List<Long> unavailableSeatIds = unavailableSeats.stream().map(Seat::getId).collect(Collectors.toList());
            throw new SeatAlreadyBookedException(String.format("Seats %s are not available", unavailableSeatIds));
        }
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
        List<Seat> unavailableSeats = seats.stream()
                .filter(seat -> seat.getSeatStatus() != SeatStatus.RESERVED && seat.getSeatStatus() != SeatStatus.AVAILABLE)
                .collect(Collectors.toList());
        if (!unavailableSeats.isEmpty()) {
            List<Long> unavailableSeatIds = unavailableSeats.stream().map(Seat::getId).collect(Collectors.toList());
            throw new SeatAlreadyBookedException(
                    String.format("Seats %s are no longer available for confirmation. Current status may be BOOKED or LOCKED", unavailableSeatIds));
        }
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.BOOKED);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
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
}
