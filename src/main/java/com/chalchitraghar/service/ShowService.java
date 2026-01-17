package com.chalchitraghar.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.dto.show.ShowRequestDto;
import com.chalchitraghar.dto.show.ShowResponseDto;
import com.chalchitraghar.exception.HallConflictException;
import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.mapper.ShowMapper;
import com.chalchitraghar.model.Hall;
import com.chalchitraghar.model.Movie;
import com.chalchitraghar.model.Show;
import com.chalchitraghar.model.enums.ShowStatus;
import com.chalchitraghar.repository.HallRepository;
import com.chalchitraghar.repository.MovieRepository;
import com.chalchitraghar.repository.ShowRepository;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ShowService {

    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final HallRepository hallRepository;
    private final ShowMapper showMapper;

    /**
     * Validates that no other show is scheduled for the hall during the given time period.
     * Rule: One show per hall at a time.
     */
    private void validateHallAvailability(Long hallId, LocalDate showDate, LocalTime showTime, LocalTime endTime, Long excludeShowId) {
        if (showRepository.existsOverlappingShow(hallId, showDate, showTime, endTime, excludeShowId)) {
            throw new HallConflictException(
                "Hall is already booked for another show during this time period. " +
                "Only one show can be scheduled per hall at a time."
            );
        }
    }

    public ShowResponseDto addShow(ShowRequestDto dto) {
        // Fetch and validate movie
        Movie movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", dto.getMovieId()));

        // Fetch and validate hall
        Hall hall = hallRepository.findById(dto.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall", dto.getHallId()));

        if (!hall.isActive()) {
            throw new HallConflictException("Cannot schedule show in an inactive hall");
        }

        // Validate hall availability - RULE: One show per hall at a time
        validateHallAvailability(
            dto.getHallId(),
            dto.getShowDate(),
            dto.getShowTime(),
            dto.getEndTime(),
            null
        );

        // Create show
        Show show = showMapper.toEntity(dto, movie, hall);
        Show saved = showRepository.save(show);
        return showMapper.toResponseDto(saved);
    }

    public ShowResponseDto updateShow(Long id, ShowRequestDto dto) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));

        // Fetch and validate movie
        Movie movie = movieRepository.findById(dto.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Movie", dto.getMovieId()));

        // Fetch and validate hall
        Hall hall = hallRepository.findById(dto.getHallId())
                .orElseThrow(() -> new ResourceNotFoundException("Hall", dto.getHallId()));

        if (!hall.isActive()) {
            throw new HallConflictException("Cannot schedule show in an inactive hall");
        }

        // Validate hall availability - exclude current show from conflict check
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

    public void deleteShow(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        show.setStatus(ShowStatus.CANCELLED);
        showRepository.save(show);
    }

    @Transactional(readOnly = true)
    public List<ShowResponseDto> getAllShows() {
        return showRepository.findAll()
                .stream()
                .map(showMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ShowResponseDto getShowById(Long id) {
        Show show = showRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Show", id));
        return showMapper.toResponseDto(show);
    }

    @Transactional(readOnly = true)
    public List<ShowResponseDto> getShowsByHall(Long hallId) {
        return showRepository.findByHallId(hallId)
                .stream()
                .map(showMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ShowResponseDto> getShowsByMovie(Long movieId) {
        return showRepository.findByMovieId(movieId)
                .stream()
                .map(showMapper::toResponseDto)
                .collect(Collectors.toList());
    }
}
