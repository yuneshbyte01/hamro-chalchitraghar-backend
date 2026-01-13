package com.chalchitraghar.service;

import java.util.List;
import java.util.stream.Collectors;

import com.chalchitraghar.dto.MovieDto;
import com.chalchitraghar.dto.MovieResponseDto;
import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.mapper.MovieMapper;
import com.chalchitraghar.model.Movie;
import com.chalchitraghar.model.enums.MovieStatus;
import com.chalchitraghar.repository.MovieRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    public MovieResponseDto addMovie(MovieDto dto) {
        Movie movie = movieMapper.toEntity(dto);
        Movie saved = movieRepository.save(movie);
        return movieMapper.toResponseDto(saved);
    }

    public MovieResponseDto updateMovie(Long id, MovieDto dto) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));

        movieMapper.updateEntityFromDto(movie, dto);
        Movie updated = movieRepository.save(movie);
        return movieMapper.toResponseDto(updated);
    }

    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movie.setStatus(MovieStatus.ENDED);
        movieRepository.save(movie);
    }

    public List<MovieResponseDto> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(movieMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    public MovieResponseDto getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        return movieMapper.toResponseDto(movie);
    }
}