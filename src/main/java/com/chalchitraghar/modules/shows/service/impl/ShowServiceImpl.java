package com.chalchitraghar.modules.shows.service.impl;

import com.chalchitraghar.modules.audit.enums.AuditAction;
import com.chalchitraghar.modules.audit.service.AuditBusinessPublisher;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.bookings.service.BookingLifecycleService;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.halls.repository.HallRepository;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.movies.repository.MovieRepository;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.shows.dto.request.ShowRequest;
import com.chalchitraghar.modules.shows.dto.request.ShowSearchCriteria;
import com.chalchitraghar.modules.shows.dto.response.AdminShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.AdminShowSummaryResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowSummaryResponse;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.shows.mapper.ShowMapper;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.modules.shows.service.SeatGenerationService;
import com.chalchitraghar.modules.shows.service.ShowLifecycleService;
import com.chalchitraghar.modules.shows.service.ShowService;
import com.chalchitraghar.modules.shows.specification.ShowSpecification;
import com.chalchitraghar.shared.exception.HallConflictException;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.exception.ShowConflictException;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowServiceImpl implements ShowService {

    private static final Set<String> SHOW_SORT_FIELDS =
            Set.of("showDate", "showTime", "endTime", "status", "createdAt", "updatedAt");

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final ShowMapper showMapper;
    private final SeatGenerationService seatGenerationService;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final ShowLifecycleService lifecycleService;
    private final Clock clock;
    private final BookingLifecycleService bookingLifecycleService;
    private final com.chalchitraghar.modules.tickets.service.TicketOperationsService
            ticketOperationsService;
    private final ApplicationEventPublisher events;
    private final AuditBusinessPublisher audit;

    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES =
            List.of(
                    BookingStatus.INITIATED,
                    BookingStatus.PENDING,
                    BookingStatus.CONFIRMED,
                    BookingStatus.BOOKED);
    private static final List<BookingStatus> SHOW_CANCELLATION_BLOCKING_STATUSES =
            List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.BOOKED);

    private Map<String, ?> showSnapshot(Show show) {
        return Map.of(
                "id", show.getId(),
                "movieId", show.getMovie().getId(),
                "hallId", show.getHall().getId(),
                "showDate", show.getShowDate().toString(),
                "showTime", show.getShowTime().toString(),
                "endTime", show.getEndTime().toString(),
                "status", show.getStatus().name());
    }

    @Value("${app.shows.buffer-minutes:15}")
    private long showBufferMinutes;

    @Value("${app.shows.duration-tolerance-minutes:5}")
    private long durationToleranceMinutes;

    private void validateHallAvailability(
            Long hallId,
            LocalDate showDate,
            LocalTime showTime,
            LocalTime endTime,
            Long excludeShowId) {
        LocalTime bufferedShowTime =
                showTime.isBefore(LocalTime.MIN.plusMinutes(showBufferMinutes))
                        ? LocalTime.MIN
                        : showTime.minusMinutes(showBufferMinutes);
        LocalTime bufferedEndTime =
                endTime.isAfter(LocalTime.MAX.minusMinutes(showBufferMinutes))
                        ? LocalTime.MAX
                        : endTime.plusMinutes(showBufferMinutes);
        if (showRepository.existsOverlappingShow(
                hallId, showDate, bufferedShowTime, bufferedEndTime, excludeShowId)) {
            throw new HallConflictException(
                    "Hall is already booked for another show during this time period, including the "
                            + showBufferMinutes
                            + "-minute cleaning buffer.");
        }
    }

    @Override
    public AdminShowDetailResponse addShow(ShowRequest dto) {
        Movie movie =
                movieRepository
                        .findById(dto.getMovieId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Movie", dto.getMovieId()));
        if (movie.getStatus() != MovieStatus.NOW_SHOWING) {
            throw new HallConflictException(
                    "Shows can only be scheduled for movies with status NOW_SHOWING");
        }
        validateSchedule(dto, movie);
        Hall hall =
                hallRepository
                        .findById(dto.getHallId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hall", dto.getHallId()));
        if (hall.getStatus() == Status.INACTIVE) {
            throw new HallConflictException("Cannot schedule show in an inactive hall");
        }
        validateHallAvailability(
                dto.getHallId(), dto.getShowDate(), dto.getShowTime(), dto.getEndTime(), null);
        Show show = showMapper.toEntity(dto, movie, hall);
        Show saved = showRepository.save(show);
        seatGenerationService.generateSeatsForShow(saved.getId());
        audit.show(AuditAction.SHOW_CREATED, saved.getId(), null, showSnapshot(saved));
        return showMapper.toAdminDetail(saved);
    }

    @Override
    public AdminShowDetailResponse updateShow(Long id, ShowRequest dto) {
        Show show =
                showRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        lifecycleService.reconcile(show);
        if (show.getStatus() != ShowStatus.SCHEDULED) {
            throw new ShowConflictException("Only scheduled shows can be updated");
        }
        if (hasActiveCustomerActivity(id)) {
            throw new ShowConflictException(
                    "Cannot update a show with active bookings or seat holds");
        }
        boolean hallChanges = !show.getHall().getId().equals(dto.getHallId());
        if (hallChanges && bookingRepository.existsByShowId(id)) {
            throw new ShowConflictException("Cannot change show hall after bookings exist");
        }
        if (hallChanges && seatRepository.existsByShowId(id)) {
            throw new ShowConflictException(
                    "Cannot change show hall after seats have been generated");
        }
        Movie movie =
                movieRepository
                        .findById(dto.getMovieId())
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Movie", dto.getMovieId()));
        if (movie.getStatus() != MovieStatus.NOW_SHOWING) {
            throw new HallConflictException(
                    "Shows can only be scheduled for movies with status NOW_SHOWING");
        }
        validateSchedule(dto, movie);
        Hall hall =
                hallRepository
                        .findById(dto.getHallId())
                        .orElseThrow(() -> new ResourceNotFoundException("Hall", dto.getHallId()));
        if (hall.getStatus() == Status.INACTIVE) {
            throw new HallConflictException("Cannot schedule show in an inactive hall");
        }
        validateHallAvailability(
                dto.getHallId(), dto.getShowDate(), dto.getShowTime(), dto.getEndTime(), id);
        Map<String, ?> before = showSnapshot(show);
        showMapper.updateEntityFromDto(show, dto, movie, hall);
        Show saved = showRepository.save(show);
        audit.show(AuditAction.SHOW_UPDATED, saved.getId(), before, showSnapshot(saved));
        return showMapper.toAdminDetail(saved);
    }

    @Override
    public void deleteShow(Long id) {
        Show show =
                showRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        lifecycleService.reconcile(show);
        if (show.getStatus() == ShowStatus.CANCELLED) {
            return;
        }
        if (show.getStatus() == ShowStatus.RUNNING || show.getStatus() == ShowStatus.COMPLETED) {
            throw new ShowConflictException("Running or completed shows cannot be cancelled");
        }
        var affectedUserIds = affectedUserIds(id);
        ticketOperationsService.revokeForShow(id, "SHOW_CANCELLED", null);
        bookingLifecycleService.cancelInitiatedBookingsForShow(id);
        transitionStatus(show, ShowStatus.CANCELLED);
        List<com.chalchitraghar.modules.seats.entity.Seat> locks =
                seatRepository.findActiveLockedSeatsByShowId(
                        id, java.time.LocalDateTime.now(clock));
        locks.forEach(this::clearSeatLock);
        seatRepository.saveAll(locks);
        showRepository.save(show);
        publishShowCancelled(show, affectedUserIds);
    }

    @Override
    public AdminShowDetailResponse updateShowStatus(Long id, ShowStatus status) {
        Show show =
                showRepository
                        .findByIdForUpdate(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        lifecycleService.reconcile(show);
        var affectedUserIds =
                status == ShowStatus.CANCELLED ? affectedUserIds(id) : List.<Long>of();
        if (status == ShowStatus.CANCELLED) {
            ticketOperationsService.revokeForShow(id, "SHOW_CANCELLED", null);
            bookingLifecycleService.cancelInitiatedBookingsForShow(id);
        }
        transitionStatus(show, status);
        Show saved = showRepository.save(show);
        if (status == ShowStatus.CANCELLED) publishShowCancelled(saved, affectedUserIds);
        return showMapper.toAdminDetail(saved);
    }

    @Override
    @Transactional
    public PageResponse<PublicShowSummaryResponse> getPublicShows(
            ShowSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        Page<Show> shows = searchShows(criteria, page, size, sortBy, sortDir, true);
        List<PublicShowSummaryResponse> content =
                shows.getContent().stream().map(showMapper::toPublicSummary).toList();
        return PageResponse.from(shows, content);
    }

    @Override
    @Transactional
    public PublicShowDetailResponse getPublicShowById(Long id) {
        lifecycleService.reconcilePersisted(id);
        Show show =
                showRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        if (!isPubliclyVisible(show)) {
            throw new ResourceNotFoundException("Show", id);
        }
        return showMapper.toPublicDetail(show);
    }

    @Override
    @Transactional
    public List<PublicShowSummaryResponse> getPublicShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId).stream()
                .peek(lifecycleService::reconcile)
                .filter(this::isPubliclyVisible)
                .map(showMapper::toPublicSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<PublicShowSummaryResponse> getPublicShowsByMovieAndShowDate(
            Long movieId, LocalDate showDate) {
        Movie movie =
                movieRepository
                        .findById(movieId)
                        .orElseThrow(() -> new ResourceNotFoundException("Movie", movieId));
        if (movie.getStatus() != MovieStatus.NOW_SHOWING) {
            return List.of();
        }
        return showRepository.findByMovieIdAndShowDate(movieId, showDate).stream()
                .peek(lifecycleService::reconcile)
                .filter(this::isPubliclyVisible)
                .map(showMapper::toPublicSummary)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AdminShowSummaryResponse> getAdminShows(
            ShowSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        Page<Show> shows = searchShows(criteria, page, size, sortBy, sortDir, false);
        List<AdminShowSummaryResponse> content =
                shows.getContent().stream().map(showMapper::toAdminSummary).toList();
        return PageResponse.from(shows, content);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminShowDetailResponse getAdminShowById(Long id) {
        Show show =
                showRepository
                        .findById(id)
                        .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        return showMapper.toAdminDetail(show);
    }

    private boolean isPubliclyVisible(Show show) {
        return show.getStatus() != ShowStatus.CANCELLED
                && show.getStatus() != ShowStatus.COMPLETED
                && show.getMovie().getStatus() == MovieStatus.NOW_SHOWING
                && show.getHall().getStatus() == Status.ACTIVE;
    }

    private Page<Show> searchShows(
            ShowSearchCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortDir,
            boolean publicOnly) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
        if (!SHOW_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy. Allowed values: " + String.join(", ", SHOW_SORT_FIELDS));
        }
        Sort.Direction direction = parseSortDirection(sortDir);
        ShowStatus status = publicOnly ? null : parseStatus(criteria.status());
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        Page<Show> shows =
                showRepository.findAll(
                        ShowSpecification.search(criteria, status, publicOnly, today, now),
                        pageable);
        if (publicOnly) {
            shows.getContent().forEach(lifecycleService::reconcile);
        }
        return shows;
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.DESC;
        }
        throw new IllegalArgumentException("Invalid sortDir. Allowed values: asc, desc");
    }

    private ShowStatus parseStatus(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(ShowStatus.values())
                .filter(status -> status.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Invalid status. Allowed values: "
                                                + String.join(
                                                        ", ",
                                                        Arrays.stream(ShowStatus.values())
                                                                .map(Enum::name)
                                                                .toList())));
    }

    private void validateSchedule(ShowRequest request, Movie movie) {
        LocalDate today = LocalDate.now(clock);
        LocalTime now = LocalTime.now(clock);
        if (request.getShowDate().isBefore(today)) {
            throw new IllegalArgumentException("Show date must not be in the past");
        }
        if (request.getShowDate().isEqual(today) && !request.getShowTime().isAfter(now)) {
            throw new IllegalArgumentException("Same-day show time must be in the future");
        }
        if (!request.getEndTime().isAfter(request.getShowTime())) {
            throw new IllegalArgumentException(
                    "End time must be after show time; zero-length and overnight shows are not supported");
        }

        long actualDurationSeconds =
                Duration.between(request.getShowTime(), request.getEndTime()).getSeconds();
        long expectedDurationSeconds = movie.getDurationMinutes() * 60L;
        long toleranceSeconds = durationToleranceMinutes * 60L;
        if (Math.abs(actualDurationSeconds - expectedDurationSeconds) > toleranceSeconds) {
            LocalTime expectedEndTime =
                    request.getShowTime().plusMinutes(movie.getDurationMinutes());
            throw new ShowConflictException(
                    "Show end time must be within "
                            + durationToleranceMinutes
                            + " minutes of the movie duration; expected approximately "
                            + expectedEndTime);
        }
    }

    private void transitionStatus(Show show, ShowStatus target) {
        ShowStatus current = show.getStatus();
        boolean allowed =
                (current == ShowStatus.SCHEDULED
                                && (target == ShowStatus.RUNNING || target == ShowStatus.CANCELLED))
                        || (current == ShowStatus.RUNNING
                                && (target == ShowStatus.COMPLETED
                                        || target == ShowStatus.CANCELLED));
        if (!allowed) {
            throw new ShowConflictException(
                    "Invalid show status transition from " + current + " to " + target);
        }
        show.setStatus(target);
    }

    private boolean hasActiveCustomerActivity(Long showId) {
        return bookingRepository.existsByShowIdAndStatusIn(showId, ACTIVE_BOOKING_STATUSES)
                || seatRepository.existsByShowIdAndSeatStatusAndLockExpiresAtAfter(
                        showId, SeatStatus.LOCKED, java.time.LocalDateTime.now(clock));
    }

    private void clearSeatLock(com.chalchitraghar.modules.seats.entity.Seat seat) {
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seat.setLockedAt(null);
        seat.setLockExpiresAt(null);
        seat.setLockedByUserId(null);
    }

    private List<Long> affectedUserIds(Long showId) {
        return ACTIVE_BOOKING_STATUSES.stream()
                .flatMap(status -> bookingRepository.findByShowIdAndStatus(showId, status).stream())
                .map(booking -> booking.getUser().getId())
                .distinct()
                .toList();
    }

    private void publishShowCancelled(Show show, List<Long> userIds) {
        var occurredAt = java.time.LocalDateTime.now(clock);
        var showDateTime = java.time.LocalDateTime.of(show.getShowDate(), show.getShowTime());
        userIds.forEach(
                userId ->
                        events.publishEvent(
                                new com.chalchitraghar.modules.notifications.event
                                        .ShowCancelledEvent(
                                        userId,
                                        show.getId(),
                                        show.getMovie().getTitle(),
                                        showDateTime,
                                        occurredAt)));
    }
}
