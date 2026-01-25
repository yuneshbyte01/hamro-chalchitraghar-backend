package com.chalchitraghar.service.movie;

import java.util.List;

import com.chalchitraghar.dto.movie.MovieRequest;
import com.chalchitraghar.dto.movie.MovieResponse;
import com.chalchitraghar.model.enums.MovieStatus;

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
