package com.chalchitraghar.modules.movies.service.impl;

import com.chalchitraghar.modules.movies.service.MovieService;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieSummaryResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieSummaryResponse;
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
    public AdminMovieDetailResponse addMovie(MovieRequest dto) {
        Movie movie = movieMapper.toEntity(dto);
        return movieMapper.toAdminDetail(movieRepository.save(movie));
    }

    @Override
    public AdminMovieDetailResponse updateMovie(Long id, MovieRequest dto) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movieMapper.updateEntityFromDto(movie, dto);
        return movieMapper.toAdminDetail(movieRepository.save(movie));
    }

    @Override
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movie.setStatus(MovieStatus.ENDED);
        movieRepository.save(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicMovieSummaryResponse> getPublicMovies() {
        return movieRepository.findAll().stream().map(movieMapper::toPublicSummary).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PublicMovieDetailResponse getPublicMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        return movieMapper.toPublicDetail(movie);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PublicMovieSummaryResponse> getPublicMoviesByStatus(MovieStatus status) {
        return movieRepository.findAllByStatusOrderByReleaseDateAsc(status).stream()
                .map(movieMapper::toPublicSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminMovieSummaryResponse> getAdminMovies() {
        return movieRepository.findAll().stream().map(movieMapper::toAdminSummary).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AdminMovieDetailResponse getAdminMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        return movieMapper.toAdminDetail(movie);
    }
}
