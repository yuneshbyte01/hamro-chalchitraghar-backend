package com.chalchitraghar.modules.bookings.service.impl;

import com.chalchitraghar.modules.bookings.service.BookingService;
import java.time.LocalDateTime;
import java.time.Clock;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.bookings.dto.request.BookingRequest;
import com.chalchitraghar.modules.bookings.dto.request.BookingSearchCriteria;
import com.chalchitraghar.modules.bookings.dto.response.AdminBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.AdminBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.CustomerBookingSummaryResponse;
import com.chalchitraghar.modules.bookings.dto.response.StaffBookingDetailResponse;
import com.chalchitraghar.modules.bookings.dto.response.StaffBookingSummaryResponse;
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
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.bookings.repository.BookingSeatRepository;
import com.chalchitraghar.modules.bookings.service.BookingLifecycleService;
import com.chalchitraghar.modules.bookings.service.BookingReferenceGenerator;
import com.chalchitraghar.modules.bookings.enums.ConfirmationSource;
import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.modules.bookings.specification.BookingSpecification;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.shared.exception.ShowConflictException;
import com.chalchitraghar.modules.shows.service.ShowLifecycleService;
import com.chalchitraghar.shared.response.PageResponse;
import com.chalchitraghar.modules.tickets.service.TicketIssuanceService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BookingServiceImpl implements BookingService {

    private static final Set<String> BOOKING_SORT_FIELDS = Set.of(
            "id", "bookingTime", "status", "createdAt", "updatedAt");

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final BookingMapper bookingMapper;
    private final ShowLifecycleService showLifecycleService;
    private final BookingLifecycleService bookingLifecycleService;
    private final BookingReferenceGenerator bookingReferenceGenerator;
    private final PaymentRepository paymentRepository;
    private final Clock clock;
    private final TicketIssuanceService ticketIssuanceService;
    private final com.chalchitraghar.modules.tickets.service.TicketOperationsService ticketOperationsService;

    @Value("${app.bookings.initiated-expiration-minutes:15}")
    private long initiatedExpirationMinutes;

    @Value("${app.bookings.currency:NPR}")
    private String bookingCurrency;

    @Value("${app.bookings.cancellation-cutoff-minutes:0}")
    private long cancellationCutoffMinutes;

    @Override
    @Transactional
    public CustomerBookingDetailResponse createBooking(BookingRequest request, User user) {
        if (!request.hasNoDuplicateSeats()) {
            throw new InvalidSeatSelectionException("Seat IDs contain duplicates");
        }
        Show show = showRepository.findById(request.getShowId())
                .orElseThrow(() -> new ResourceNotFoundException("Show", request.getShowId()));
        showLifecycleService.assertBookable(show);
        List<Long> orderedSeatIds = request.getSeatIds().stream().sorted().toList();
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(request.getShowId(), orderedSeatIds);
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
        LocalDateTime bookingTime = LocalDateTime.now(clock);
        BigDecimal totalAmount = seats.stream().map(Seat::getPrice).reduce(BigDecimal.ZERO, BigDecimal::add);
        Booking booking = Booking.builder()
                .user(user)
                .show(show)
                .bookingReference(bookingReferenceGenerator.generate())
                .bookingTime(bookingTime)
                .status(BookingStatus.INITIATED)
                .totalAmount(totalAmount)
                .currency(bookingCurrency)
                .expiresAt(bookingTime.plusMinutes(initiatedExpirationMinutes))
                .build();
        booking = bookingRepository.save(booking);
        List<BookingSeat> bookingSeats = new ArrayList<>();
        for (Seat seat : seats) {
            bookingSeats.add(BookingSeat.builder().booking(booking).seat(seat).unitPrice(seat.getPrice()).build());
        }
        bookingSeatRepository.saveAll(bookingSeats);
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.RESERVED);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
            seat.setLockedByUserId(null);
        }
        seatRepository.saveAll(seats);
        return bookingMapper.toCustomerDetail(booking, bookingSeats);
    }

    @Override
    @Transactional
    public CustomerBookingDetailResponse confirmBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    String.format("Booking %d does not belong to user %d", bookingId, user.getId()));
        }
        if (booking.getStatus() == BookingStatus.CONFIRMED) {
            ticketIssuanceService.issueTicketsForConfirmedBooking(booking);
            return bookingMapper.toCustomerDetail(booking, seatsFor(bookingId));
        }
        if (bookingLifecycleService.reconcileExpiry(booking)) {
            throw new InvalidBookingStateException("Booking has expired and cannot be confirmed");
        }
        if (booking.getStatus() != BookingStatus.INITIATED) {
            throw new InvalidBookingStateException(
                    bookingId, booking.getStatus().name(), BookingStatus.INITIATED.name());
        }
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        if (bookingSeats.isEmpty()) {
            throw new ResourceNotFoundException(String.format("No seats found for booking %d", bookingId));
        }
        List<Long> seatIds = bookingSeats.stream().map(bs -> bs.getSeat().getId()).sorted().toList();
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
        showLifecycleService.assertBookable(booking.getShow());
        if (paymentRepository.findFirstByBookingIdAndStatusOrderByCompletedAtDesc(
                bookingId, com.chalchitraghar.modules.payments.enums.PaymentStatus.SUCCESS).isEmpty()) {
            throw new InvalidBookingStateException("Successful payment is required before booking confirmation");
        }
        for (Seat seat : seats) {
            seat.setSeatStatus(SeatStatus.BOOKED);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
            seat.setLockedByUserId(null);
        }
        seatRepository.saveAll(seats);
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setConfirmedAt(LocalDateTime.now(clock));
        booking.setConfirmationSource(ConfirmationSource.CUSTOMER);
        bookingRepository.save(booking);
        ticketIssuanceService.issueTicketsForConfirmedBooking(booking);
        return bookingMapper.toCustomerDetail(booking, bookingSeats);
    }

    @Override
    @Transactional
    public PageResponse<CustomerBookingSummaryResponse> getCustomerBookings(
            User user, BookingSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        return searchBookings(criteria, user.getId(), page, size, sortBy, sortDir,
                bookingMapper::toCustomerSummary);
    }

    @Override
    @Transactional
    public CustomerBookingDetailResponse cancelBooking(Long bookingId, User user) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException(
                    String.format("Booking %d does not belong to user %d", bookingId, user.getId()));
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            return bookingMapper.toCustomerDetail(booking, seatsFor(bookingId));
        }
        bookingLifecycleService.reconcileExpiry(booking);
        if (booking.getStatus() != BookingStatus.INITIATED) {
            throw new InvalidBookingStateException(
                    String.format("Booking %d cannot be cancelled. Current status: %s. Only INITIATED bookings can be cancelled.",
                            bookingId, booking.getStatus().name()));
        }
        Show show = booking.getShow();
        LocalDateTime showDateTime = LocalDateTime.of(show.getShowDate(), show.getShowTime());
        LocalDateTime cancellationDeadline = showDateTime.minusMinutes(cancellationCutoffMinutes);
        if (!LocalDateTime.now(clock).isBefore(cancellationDeadline)) {
            throw new InvalidBookingStateException(
                    String.format("Booking %d cannot be cancelled. Show time has passed or is in progress. Show time: %s",
                            bookingId, showDateTime));
        }
        List<BookingSeat> bookingSeats = bookingSeatRepository.findByBookingId(bookingId);
        if (bookingSeats.isEmpty()) {
            throw new ResourceNotFoundException(String.format("No seats found for booking %d", bookingId));
        }
        List<Long> seatIds = bookingSeats.stream().map(bs -> bs.getSeat().getId()).sorted().toList();
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(booking.getShow().getId(), seatIds);
        if (seats.size() != seatIds.size()) {
            List<Long> foundSeatIds = seats.stream().map(Seat::getId).collect(Collectors.toList());
            List<Long> missingSeatIds = seatIds.stream().filter(id -> !foundSeatIds.contains(id)).collect(Collectors.toList());
            throw new ResourceNotFoundException(String.format("Seats not found: %s", missingSeatIds));
        }
        for (Seat seat : seats) {
            if (seat.getSeatStatus() == SeatStatus.RESERVED) {
                seat.setSeatStatus(SeatStatus.AVAILABLE);
                seat.setLockedAt(null);
                seat.setLockExpiresAt(null);
                seat.setLockedByUserId(null);
            }
        }
        seatRepository.saveAll(seats);
        booking.setStatus(BookingStatus.CANCELLED);
        booking.setCancelledAt(LocalDateTime.now(clock));
        bookingRepository.save(booking);
        ticketOperationsService.revokeForBooking(bookingId, "BOOKING_CANCELLED", user);
        return bookingMapper.toCustomerDetail(booking, bookingSeats);
    }

    @Override
    @Transactional
    public CustomerBookingDetailResponse getCustomerBookingById(Long bookingId, User user) {
        Booking booking = findBooking(bookingId);
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("You do not have access to this booking");
        }
        return bookingMapper.toCustomerDetail(booking, seatsFor(bookingId));
    }

    @Override
    @Transactional
    public CustomerBookingDetailResponse getCustomerBookingByReference(String reference, User user) {
        Booking booking = findBooking(reference);
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Booking not found with reference: " + reference);
        }
        return bookingMapper.toCustomerDetail(booking, seatsFor(booking.getId()));
    }

    @Override
    @Transactional
    public StaffBookingDetailResponse getStaffBookingById(Long bookingId) {
        Booking booking = findBooking(bookingId);
        return bookingMapper.toStaffDetail(booking, seatsFor(bookingId));
    }

    @Override
    @Transactional
    public StaffBookingDetailResponse getStaffBookingByReference(String reference) {
        Booking booking = findBooking(reference);
        return bookingMapper.toStaffDetail(booking, seatsFor(booking.getId()));
    }

    @Override
    @Transactional
    public PageResponse<StaffBookingSummaryResponse> getStaffBookings(
            BookingSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        return searchBookings(criteria, null, page, size, sortBy, sortDir, bookingMapper::toStaffSummary);
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse getAdminBookingById(Long bookingId) {
        Booking booking = findBooking(bookingId);
        return bookingMapper.toAdminDetail(booking, seatsFor(bookingId));
    }

    @Override
    @Transactional
    public AdminBookingDetailResponse getAdminBookingByReference(String reference) {
        Booking booking = findBooking(reference);
        return bookingMapper.toAdminDetail(booking, seatsFor(booking.getId()));
    }

    @Override
    @Transactional
    public PageResponse<AdminBookingSummaryResponse> getAdminBookings(
            BookingSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        return searchBookings(criteria, null, page, size, sortBy, sortDir, bookingMapper::toAdminSummary);
    }

    private <T> PageResponse<T> searchBookings(
            BookingSearchCriteria criteria,
            Long ownerId,
            int page,
            int size,
            String sortBy,
            String sortDir,
            BiFunction<Booking, List<BookingSeat>, T> mapper) {
        validateSearch(criteria, page, size, sortBy, sortDir);
        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        BookingStatus status = parseStatus(criteria.status());
        Page<Booking> bookings = bookingRepository.findAll(
                BookingSpecification.search(criteria, status, ownerId),
                PageRequest.of(page, size, Sort.by(direction, sortBy)));
        bookings.getContent().forEach(bookingLifecycleService::reconcileExpiry);
        Map<Long, List<BookingSeat>> seatsByBooking = bookingSeatRepository.findByBookingIdsWithSeats(
                        bookings.getContent().stream().map(Booking::getId).toList()).stream()
                .collect(Collectors.groupingBy(bs -> bs.getBooking().getId()));
        List<T> content = bookings.getContent().stream()
                .map(booking -> mapper.apply(booking, seatsByBooking.getOrDefault(booking.getId(), List.of())))
                .toList();
        return PageResponse.from(bookings, content);
    }

    private void validateSearch(
            BookingSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        if (page < 0) throw new IllegalArgumentException("Page must be zero or greater");
        if (size < 1) throw new IllegalArgumentException("Size must be at least 1");
        if (!BOOKING_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy. Allowed values: "
                    + String.join(", ", BOOKING_SORT_FIELDS));
        }
        if (!"asc".equalsIgnoreCase(sortDir) && !"desc".equalsIgnoreCase(sortDir)) {
            throw new IllegalArgumentException("Invalid sortDir. Allowed values: asc, desc");
        }
        if (criteria.showDateFrom() != null && criteria.showDateTo() != null
                && criteria.showDateFrom().isAfter(criteria.showDateTo())) {
            throw new IllegalArgumentException("showDateFrom must not be after showDateTo");
        }
        if (criteria.bookingTimeFrom() != null && criteria.bookingTimeTo() != null
                && criteria.bookingTimeFrom().isAfter(criteria.bookingTimeTo())) {
            throw new IllegalArgumentException("bookingTimeFrom must not be after bookingTimeTo");
        }
    }

    private BookingStatus parseStatus(String value) {
        if (value == null || value.isBlank()) return null;
        return Arrays.stream(BookingStatus.values())
                .filter(status -> status.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid status. Allowed values: "
                        + String.join(", ", Arrays.stream(BookingStatus.values()).map(Enum::name).toList())));
    }

    private Booking findBooking(Long bookingId) {
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId));
        bookingLifecycleService.reconcileExpiry(booking);
        return booking;
    }

    private Booking findBooking(String reference) {
        Booking booking = bookingRepository.findByBookingReference(reference.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with reference: " + reference));
        bookingLifecycleService.reconcileExpiry(booking);
        return booking;
    }

    private List<BookingSeat> seatsFor(Long bookingId) {
        return bookingSeatRepository.findByBookingId(bookingId);
    }

    private void validateSeatsForBookingCreation(List<Seat> seats, Long userId) {
        LocalDateTime now = LocalDateTime.now(clock);
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
