package com.chalchitraghar.modules.movies;

import java.util.List;

import com.chalchitraghar.modules.movies.MovieRequest;
import com.chalchitraghar.modules.movies.MovieResponse;
import com.chalchitraghar.modules.movies.MovieStatus;

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
