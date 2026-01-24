package com.chalchitraghar.service;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
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

/**
 * Service for managing booking operations.
 */
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final BookingMapper bookingMapper;

    /**
     * Creates a new booking for the authenticated user.
     * Validates show existence, seat availability, and creates booking with seat mappings.
     * All operations are performed within a single transaction.
     *
     * @param request the booking request containing showId and seatIds
     * @param user the authenticated user making the booking
     * @return the created booking response
     * @throws ResourceNotFoundException if show or any seat is not found
     * @throws InvalidSeatSelectionException if seats don't belong to the show or have duplicates
     * @throws SeatAlreadyBookedException if any seat is not available
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest request, User user) {
        // Validate no duplicate seats
        if (!request.hasNoDuplicateSeats()) {
            throw new InvalidSeatSelectionException("Seat IDs contain duplicates");
        }

        // Validate and fetch show
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", request.getShowId()));

        // Fetch seats with pessimistic lock to prevent concurrent modifications
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(
                request.getShowId(), request.getSeatIds());

        // Validate all seats exist
        if (seats.size() != request.getSeatIds().size()) {
            List<Long> foundSeatIds = seats.stream()
                    .map(Seat::getId)
                    .collect(Collectors.toList());
            List<Long> missingSeatIds = request.getSeatIds().stream()
                    .filter(id -> !foundSeatIds.contains(id))
                    .collect(Collectors.toList());
            throw new ResourceNotFoundException(
                    String.format("Seats not found: %s", missingSeatIds));
        }

        // Validate all seats belong to the show
        List<Seat> invalidSeats = seats.stream()
                .filter(seat -> !seat.getShow().getId().equals(request.getShowId()))
                .collect(Collectors.toList());
        if (!invalidSeats.isEmpty()) {
            throw new InvalidSeatSelectionException(
                    String.format("Seats %s do not belong to show %d",
                            invalidSeats.stream()
                                    .map(Seat::getId)
                                    .collect(Collectors.toList()),
                            request.getShowId()));
        }

        // Check for existing bookings for these seats
        List<BookingSeat> existingBookings = bookingSeatRepository.findBySeatIds(request.getSeatIds());
        if (!existingBookings.isEmpty()) {
            List<Long> bookedSeatIds = existingBookings.stream()
                    .map(bs -> bs.getSeat().getId())
                    .collect(Collectors.toList());
            throw new SeatAlreadyBookedException(
                    String.format("Seats %s are already booked", bookedSeatIds));
        }

        // Validate all seats are AVAILABLE (not BOOKED, RESERVED, or actively LOCKED)
        List<Seat> unavailableSeats = seats.stream()
                .filter(seat -> {
                    // Check if seat is already BOOKED
                    if (seat.getSeatStatus() == SeatStatus.BOOKED) {
                        return true;
                    }
                    // Check if seat is RESERVED (already part of another INITIATED booking)
                    if (seat.getSeatStatus() == SeatStatus.RESERVED) {
                        return true;
                    }
                    // Check if seat is LOCKED and lock hasn't expired
                    if (seat.getSeatStatus() == SeatStatus.LOCKED) {
                        if (seat.getLockExpiresAt() != null && seat.getLockExpiresAt().isAfter(LocalDateTime.now())) {
                            return true; // Still locked
                        }
                    }
                    // Allow AVAILABLE or expired LOCKED seats
                    return false;
                })
                .collect(Collectors.toList());
        if (!unavailableSeats.isEmpty()) {
            List<Long> unavailableSeatIds = unavailableSeats.stream()
                    .map(Seat::getId)
                    .collect(Collectors.toList());
            throw new SeatAlreadyBookedException(
                    String.format("Seats %s are not available", unavailableSeatIds));
        }

        // Create booking
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .bookingTime(LocalDateTime.now())
                .status(BookingStatus.INITIATED)
                .build();
        booking = bookingRepository.save(booking);

        // Create booking-seat mappings
        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (Seat seat : seats) {
            BookingSeat bookingSeat = BookingSeat.builder()
                    .booking(booking)
                    .seat(seat)
                    .build();
            bookingSeats.add(bookingSeat);
        }
        bookingSeatRepository.saveAll(bookingSeats);

        // Mark seats as RESERVED when booking is initiated
        // This prevents other users from booking the same seats
        // Seats will be updated to BOOKED when booking is confirmed (Mission 5.4)
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.RESERVED);
            // Clear any temporary lock information since seat is now reserved for this booking
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
        }
        seatRepository.saveAll(seats);

        // Map booking and seats to response DTO
        return bookingMapper.toResponseDto(booking, seats);
    }

    /**
     * Confirms a booking and permanently locks the associated seats.
     * Validates booking ownership, status, and seat availability before confirmation.
     * All operations are performed within a single transaction.
     *
     * @param bookingId the ID of the booking to confirm
     * @param user the authenticated user attempting to confirm the booking
     * @return the confirmed booking response
     * @throws ResourceNotFoundException if booking is not found
     * @throws AccessDeniedException if booking does not belong to the user
     * @throws InvalidBookingStateException if booking is not in INITIATED state
     * @throws SeatAlreadyBookedException if any seat is already booked
     */
    @Transactional
    public BookingResponse confirmBooking(Long bookingId, User user) {
        // Fetch booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Validate booking ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    String.format("Booking %d does not belong to user %d", bookingId, user.getId()));
        }

        // Validate booking status is INITIATED
        if (booking.getStatus() != BookingStatus.INITIATED) {
            throw new InvalidBookingStateException(
                    bookingId,
                    booking.getStatus().name(),
                    BookingStatus.INITIATED.name());
        }

        // Fetch all associated seats for this booking
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        if (bookingSeats.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format("No seats found for booking %d", bookingId));
        }

        // Extract seat IDs
        List<Long> seatIds = bookingSeats.stream()
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toList());

        // Re-fetch seats with pessimistic lock to prevent concurrent modifications
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(
                booking.getShow().getId(), seatIds);

        // Validate all seats exist
        if (seats.size() != seatIds.size()) {
            List<Long> foundSeatIds = seats.stream()
                    .map(Seat::getId)
                    .collect(Collectors.toList());
            List<Long> missingSeatIds = seatIds.stream()
                    .filter(id -> !foundSeatIds.contains(id))
                    .collect(Collectors.toList());
            throw new ResourceNotFoundException(
                    String.format("Seats not found: %s", missingSeatIds));
        }

        // Check for other CONFIRMED bookings for these seats
        List<BookingSeat> existingConfirmedBookings = bookingSeatRepository
                .findConfirmedBookingsBySeatIds(seatIds, bookingId);
        if (!existingConfirmedBookings.isEmpty()) {
            List<Long> bookedSeatIds = existingConfirmedBookings.stream()
                    .map(bs -> bs.getSeat().getId())
                    .distinct()
                    .collect(Collectors.toList());
            throw new SeatAlreadyBookedException(
                    String.format("Seats %s are already confirmed in another booking", bookedSeatIds));
        }

        // Validate all seats are still RESERVED (for this booking) or AVAILABLE
        // Seats should be RESERVED since they were reserved when booking was created
        // But we also allow AVAILABLE in case of edge cases
        List<Seat> unavailableSeats = seats.stream()
                .filter(seat -> {
                    // Allow RESERVED (expected state) or AVAILABLE (edge case)
                    if (seat.getSeatStatus() == SeatStatus.RESERVED || seat.getSeatStatus() == SeatStatus.AVAILABLE) {
                        return false; // Available for confirmation
                    }
                    // Reject if BOOKED, LOCKED, or any other status
                    return true;
                })
                .collect(Collectors.toList());
        if (!unavailableSeats.isEmpty()) {
            List<Long> unavailableSeatIds = unavailableSeats.stream()
                    .map(Seat::getId)
                    .collect(Collectors.toList());
            throw new SeatAlreadyBookedException(
                    String.format("Seats %s are no longer available for confirmation. Current status may be BOOKED or LOCKED", unavailableSeatIds));
        }

        // Update all seats to BOOKED and clear lock information
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.BOOKED);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
        }
        seatRepository.saveAll(seats);

        // Update booking status to CONFIRMED
        booking.setStatus(BookingStatus.CONFIRMED);
        booking = bookingRepository.save(booking);

        // Map booking and seats to response DTO
        return bookingMapper.toResponseDto(booking, seats);
    }

    /**
     * Retrieves all bookings for the authenticated user.
     * Returns bookings sorted by booking time descending (most recent first).
     *
     * @param user the authenticated user
     * @return list of booking responses for the user
     */
    @Transactional(readOnly = true)
    public List<BookingResponse> getMyBookings(User user) {
        // Fetch all bookings for the user, sorted by bookingTime descending
        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookingTimeDesc(user.getId());

        // Map each booking to response DTO
        return bookings.stream()
                .map(booking -> {
                    // Fetch associated seats for this booking
                    List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(booking.getId());
                    List<Seat> seats = bookingSeats.stream()
                            .map(BookingSeat::getSeat)
                            .collect(Collectors.toList());
                    
                    // Map to response DTO
                    return bookingMapper.toResponseDto(booking, seats);
                })
                .collect(Collectors.toList());
    }

    /**
     * Cancels a booking and releases all associated seats.
     * Validates booking ownership, status, and cancellation eligibility before cancellation.
     * Only INITIATED bookings can be cancelled. CONFIRMED bookings cannot be cancelled.
     * All operations are performed within a single transaction.
     *
     * @param bookingId the ID of the booking to cancel
     * @param user the authenticated user attempting to cancel the booking
     * @return the cancelled booking response
     * @throws ResourceNotFoundException if booking is not found
     * @throws AccessDeniedException if booking does not belong to the user
     * @throws InvalidBookingStateException if booking is not eligible for cancellation (not INITIATED or showtime has passed)
     */
    @Transactional
    public BookingResponse cancelBooking(Long bookingId, User user) {
        // Fetch booking
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));

        // Validate booking ownership
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    String.format("Booking %d does not belong to user %d", bookingId, user.getId()));
        }

        // Validate booking status is INITIATED (only INITIATED bookings can be cancelled)
        if (booking.getStatus() != BookingStatus.INITIATED) {
            throw new InvalidBookingStateException(
                    String.format("Booking %d cannot be cancelled. Current status: %s. Only INITIATED bookings can be cancelled.",
                            bookingId, booking.getStatus().name()));
        }

        // Validate cancellation window: booking can only be cancelled before showtime
        Show show = booking.getShow();
        LocalDate showDate = show.getShowDate();
        LocalTime showTime = show.getShowTime();
        LocalDateTime showDateTime = LocalDateTime.of(showDate, showTime);
        LocalDateTime now = LocalDateTime.now();

        if (now.isAfter(showDateTime) || now.isEqual(showDateTime)) {
            throw new InvalidBookingStateException(
                    String.format("Booking %d cannot be cancelled. Show time has passed or is in progress. Show time: %s",
                            bookingId, showDateTime));
        }

        // Fetch all associated seats for this booking
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        if (bookingSeats.isEmpty()) {
            throw new ResourceNotFoundException(
                    String.format("No seats found for booking %d", bookingId));
        }

        // Extract seat IDs
        List<Long> seatIds = bookingSeats.stream()
                .map(bs -> bs.getSeat().getId())
                .collect(Collectors.toList());

        // Re-fetch seats with pessimistic lock to prevent concurrent modifications
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(
                booking.getShow().getId(), seatIds);

        // Validate all seats exist
        if (seats.size() != seatIds.size()) {
            List<Long> foundSeatIds = seats.stream()
                    .map(Seat::getId)
                    .collect(Collectors.toList());
            List<Long> missingSeatIds = seatIds.stream()
                    .filter(id -> !foundSeatIds.contains(id))
                    .collect(Collectors.toList());
            throw new ResourceNotFoundException(
                    String.format("Seats not found: %s", missingSeatIds));
        }

        // Release all seats: set status to AVAILABLE
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            // Clear any lock information
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
        }
        seatRepository.saveAll(seats);

        // Update booking status to CANCELLED
        booking.setStatus(BookingStatus.CANCELLED);
        booking = bookingRepository.save(booking);

        // Map booking and seats to response DTO
        return bookingMapper.toResponseDto(booking, seats);
    }

    /**
     * Fetches a booking by ID. Customers may only fetch their own; STAFF and ADMIN may fetch any.
     *
     * @param bookingId the booking ID
     * @param user the authenticated user
     * @return the booking response
     * @throws ResourceNotFoundException if booking is not found
     * @throws AccessDeniedException if customer tries to access another user's booking
     */
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
        List<Seat> seats = bookingSeats.stream()
                .map(BookingSeat::getSeat)
                .collect(Collectors.toList());
        return bookingMapper.toResponseDto(booking, seats);
    }
}
