package com.chalchitraghar.modules.movies.service.impl;

import com.chalchitraghar.modules.movies.service.MovieService;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.response.MovieResponse;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.modules.movies.mapper.MovieMapper;
import com.chalchitraghar.modules.movies.entity.Movie;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.movies.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class MovieServiceImpl implements MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    @Override
    public MovieResponse addMovie(MovieRequest dto) {
        Movie movie = movieMapper.toEntity(dto);
        return movieMapper.toResponseDto(movieRepository.save(movie));
    }

    @Override
    public MovieResponse updateMovie(Long id, MovieRequest dto) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movieMapper.updateEntityFromDto(movie, dto);
        return movieMapper.toResponseDto(movieRepository.save(movie));
    }

    @Override
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movie.setStatus(MovieStatus.ENDED);
        movieRepository.save(movie);
    }

    @Override
    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll().stream().map(movieMapper::toResponseDto).collect(Collectors.toList());
    }

    @Override
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        return movieMapper.toResponseDto(movie);
    }

    @Override
    public List<MovieResponse> getMoviesByStatus(MovieStatus status) {
        return movieRepository.findAllByStatusOrderByReleaseDateAsc(status).stream()
                .map(movieMapper::toResponseDto).toList();
    }
}
