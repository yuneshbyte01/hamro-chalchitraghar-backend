package com.chalchitraghar.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.dto.movie.MovieRequest;
import com.chalchitraghar.dto.movie.MovieResponse;
import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.mapper.MovieMapper;
import com.chalchitraghar.model.Movie;
import com.chalchitraghar.model.enums.MovieStatus;
import com.chalchitraghar.repository.MovieRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for movie management operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;

    /**
     * Creates a new movie.
     *
     * @param dto movie request containing movie details
     * @return created movie response
     */
    public MovieResponse addMovie(MovieRequest dto) {
        Movie movie = movieMapper.toEntity(dto);
        Movie saved = movieRepository.save(movie);
        return movieMapper.toResponseDto(saved);
    }

    public MovieResponse updateMovie(Long id, MovieRequest dto) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));

        movieMapper.updateEntityFromDto(movie, dto);
        Movie updated = movieRepository.save(movie);
        return movieMapper.toResponseDto(updated);
    }

    /**
     * Soft deletes a movie by setting its status to ENDED.
     *
     * @param id the movie ID
     * @throws ResourceNotFoundException if movie is not found
     */
    public void deleteMovie(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        movie.setStatus(MovieStatus.ENDED);
        movieRepository.save(movie);
    }

    public List<MovieResponse> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(movieMapper::toResponseDto)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a movie by ID.
     *
     * @param id the movie ID
     * @return movie response
     * @throws ResourceNotFoundException if movie is not found
     */
    public MovieResponse getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Movie", id));
        return movieMapper.toResponseDto(movie);
    }

    /**
     * Retrieves movies filtered by status, ordered by release date ascending.
     *
     * @param status the movie status to filter by
     * @return list of movie responses matching the status
     */
    public List<MovieResponse> getMoviesByStatus(MovieStatus status) {

        List<Movie> movies = movieRepository.findAllByStatusOrderByReleaseDateAsc(status);
    
        return movies.stream()
                .map(movieMapper::toResponseDto)
                .toList();
    }
}