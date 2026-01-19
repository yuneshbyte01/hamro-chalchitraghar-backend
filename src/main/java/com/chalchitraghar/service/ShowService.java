package com.chalchitraghar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.dto.show.ShowRequest;
import com.chalchitraghar.dto.show.ShowResponse;
import com.chalchitraghar.exception.HallConflictException;
import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.mapper.ShowMapper;
import com.chalchitraghar.model.Hall;
import com.chalchitraghar.model.Movie;
import com.chalchitraghar.model.Show;
import com.chalchitraghar.model.enums.MovieStatus;
import com.chalchitraghar.model.enums.ShowStatus;
import com.chalchitraghar.model.enums.Status;
import com.chalchitraghar.repository.HallRepository;
import com.chalchitraghar.repository.MovieRepository;
import com.chalchitraghar.repository.ShowRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for show management operations.
 * Handles show scheduling with validation for hall availability and movie status.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final ShowMapper showMapper;
    private final SeatGenerationService seatGenerationService;

    /**
     * Validates that no other show is scheduled for the hall during the given time period.
     * Rule: One show per hall at a time.
     *
     * @param hallId the hall ID
     * @param showDate the show date
     * @param showTime the start time
     * @param endTime the end time
     * @param excludeShowId optional show ID to exclude from conflict check (for updates)
     * @throws HallConflictException if an overlapping show exists
     */
    private void validateHallAvailability(Long hallId, LocalDate showDate, LocalTime showTime, LocalTime endTime, Long excludeShowId) {
        if (showRepository.existsOverlappingShow(hallId, showDate, showTime, endTime, excludeShowId)) {
            throw new HallConflictException(
                "Hall is already booked for another show during this time period. " +
                "Only one show can be scheduled per hall at a time."
            );
        }
    }

    /**
     * Creates a new show and generates seats for it.
     *
     * @param dto show request containing show details
     * @return created show response
     * @throws ResourceNotFoundException if movie or hall is not found
     * @throws HallConflictException if movie status is invalid, hall is inactive, or hall is already booked
     */
    public ShowResponse addShow(ShowRequest dto) {
        Movie movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", dto.getMovieId()));

        if (movie.getStatus() != MovieStatus.NOW_SHOWING) {
            throw new HallConflictException("Shows can only be scheduled for movies with status NOW_SHOWING");
        }

        Hall hall = hallRepository.findById(dto.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall", dto.getHallId()));

        if (hall.getStatus() == Status.INACTIVE) {
            throw new HallConflictException("Cannot schedule show in an inactive hall");
        }

        validateHallAvailability(
            dto.getHallId(),
            dto.getShowDate(),
            dto.getShowTime(),
            dto.getEndTime(),
            null
        );

        Show show = showMapper.toEntity(dto, movie, hall);
        Show saved = showRepository.save(show);
        seatGenerationService.generateSeatsForShow(saved.getId());
        return showMapper.toResponseDto(saved);
    }

    public ShowResponse updateShow(Long id, ShowRequest dto) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));

        Movie movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", dto.getMovieId()));

        if (movie.getStatus() != MovieStatus.NOW_SHOWING) {
            throw new HallConflictException("Shows can only be scheduled for movies with status NOW_SHOWING");
        }

        Hall hall = hallRepository.findById(dto.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall", dto.getHallId()));

        if (hall.getStatus() == Status.INACTIVE) {
            throw new HallConflictException("Cannot schedule show in an inactive hall");
        }

        validateHallAvailability(
            dto.getHallId(),
            dto.getShowDate(),
            dto.getShowTime(),
            dto.getEndTime(),
            id
        );

        // Update show
        showMapper.updateEntityFromDto(show, dto, movie, hall);
        Show updated = showRepository.save(show);
        return showMapper.toResponseDto(updated);
    }

    /**
     * Soft deletes a show by setting its status to CANCELLED.
     *
     * @param id the show ID
     * @throws ResourceNotFoundException if show is not found
     */
    public void deleteShow(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);
    }

    /**
     * Retrieves all shows.
     *
     * @return list of all show responses
     */
    @Transactional(readOnly = true)
    public List<ShowResponse> getAllShows() {
        return showRepository.findAll()
                .stream()
                .map(showMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a show by ID, only if it's active and the movie is NOW_SHOWING.
     *
     * @param id the show ID
     * @return show response
     * @throws ResourceNotFoundException if show is not found, cancelled, completed, or movie is not NOW_SHOWING
     */
    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        
        if (show.getStatus() == ShowStatus.CANCELLED || show.getStatus() == ShowStatus.COMPLETED) {
            throw new ResourceNotFoundException("Show", id);
        }
        
        if (show.getMovie().getStatus() != MovieStatus.NOW_SHOWING) {
            throw new ResourceNotFoundException("Show", id);
        }
        
        return showMapper.toResponseDto(show);
    }

    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByHall(Long hallId) {
        return showRepository.findByHallId(hallId)
                .stream()
                .map(showMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves all shows for a specific movie.
     *
     * @param movieId the movie ID
     * @return list of show responses for the movie
     */
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId)
                .stream()
                .map(showMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves shows for a specific movie on a specific date.
     * Only returns shows for NOW_SHOWING movies and excludes cancelled/completed shows.
     *
     * @param movieId the movie ID
     * @param showDate the show date
     * @return list of show responses matching the criteria
     * @throws ResourceNotFoundException if movie is not found
     */
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovieAndShowDate(Long movieId, LocalDate showDate) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", movieId));
        
        if (movie.getStatus() != MovieStatus.NOW_SHOWING) {
            return List.of();
        }
        
        return showRepository.findByMovieIdAndShowDate(movieId, showDate)
                .stream()
                .filter(show -> show.getStatus() != ShowStatus.CANCELLED && show.getStatus() != ShowStatus.COMPLETED)
                .map(showMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
