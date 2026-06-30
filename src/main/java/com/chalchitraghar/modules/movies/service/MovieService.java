package com.chalchitraghar.modules.movies.service;

import java.util.List;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.response.MovieResponse;
import com.chalchitraghar.modules.movies.enums.MovieStatus;

/**
 * Service for movie management operations.
 */
public interface MovieService {

    MovieResponse addMovie(MovieRequest dto);

    MovieResponse updateMovie(Long id, MovieRequest dto);

    void deleteMovie(Long id);

    List<MovieResponse> getAllMovies();

    MovieResponse getMovieById(Long id);

    List<MovieResponse> getMoviesByStatus(MovieStatus status);
}
