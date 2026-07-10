package com.chalchitraghar.modules.shows.service.impl;

import com.chalchitraghar.modules.shows.service.SeatGenerationService;
import com.chalchitraghar.modules.shows.service.ShowService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.shows.dto.request.ShowRequest;
import com.chalchitraghar.modules.shows.dto.response.ShowResponse;
import com.chalchitraghar.shared.exception.HallConflictException;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.modules.shows.mapper.ShowMapper;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.halls.repository.HallRepository;
import com.chalchitraghar.modules.movies.repository.MovieRepository;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.shared.exception.ShowConflictException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowServiceImpl implements ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final ShowMapper showMapper;
    private final SeatGenerationService seatGenerationService;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;

    private void validateHallAvailability(Long hallId, LocalDate showDate, LocalTime showTime, LocalTime endTime, Long excludeShowId) {
        if (showRepository.existsOverlappingShow(hallId, showDate, showTime, endTime, excludeShowId)) {
            throw new HallConflictException(
                    "Hall is already booked for another show during this time period. " +
                    "Only one show can be scheduled per hall at a time.");
        }
    }

    @Override
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
        validateHallAvailability(dto.getHallId(), dto.getShowDate(), dto.getShowTime(), dto.getEndTime(), null);
        Show show = showMapper.toEntity(dto, movie, hall);
        Show saved = showRepository.save(show);
        seatGenerationService.generateSeatsForShow(saved.getId());
        return showMapper.toResponseDto(saved);
    }

    @Override
    public ShowResponse updateShow(Long id, ShowRequest dto) {
        Show show = showRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show", id));
        boolean hallChanges = !show.getHall().getId().equals(dto.getHallId());
        if (hallChanges && bookingRepository.existsByShowId(id)) {
            throw new ShowConflictException("Cannot change show hall after bookings exist");
        }
        if (hallChanges && seatRepository.existsByShowId(id)) {
            throw new ShowConflictException("Cannot change show hall after seats have been generated");
        }
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
        validateHallAvailability(dto.getHallId(), dto.getShowDate(), dto.getShowTime(), dto.getEndTime(), id);
        showMapper.updateEntityFromDto(show, dto, movie, hall);
        return showMapper.toResponseDto(showRepository.save(show));
    }

    @Override
    public void deleteShow(Long id) {
        Show show = showRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show", id));
        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getAllShows() {
        return showRepository.findAll().stream().map(showMapper::toResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ShowResponse getShowById(Long id) {
        Show show = showRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Show", id));
        if (show.getStatus() == ShowStatus.CANCELLED || show.getStatus() == ShowStatus.COMPLETED) {
            throw new ResourceNotFoundException("Show", id);
        }
        if (show.getMovie().getStatus() != MovieStatus.NOW_SHOWING) {
            throw new ResourceNotFoundException("Show", id);
        }
        return showMapper.toResponseDto(show);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByHall(Long hallId) {
        return showRepository.findByHallId(hallId).stream().map(showMapper::toResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId).stream().map(showMapper::toResponseDto).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ShowResponse> getShowsByMovieAndShowDate(Long movieId, LocalDate showDate) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", movieId));
        if (movie.getStatus() != MovieStatus.NOW_SHOWING) {
            return List.of();
        }
        return showRepository.findByMovieIdAndShowDate(movieId, showDate).stream()
                .filter(show -> show.getStatus() != ShowStatus.CANCELLED && show.getStatus() != ShowStatus.COMPLETED)
                .map(showMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
