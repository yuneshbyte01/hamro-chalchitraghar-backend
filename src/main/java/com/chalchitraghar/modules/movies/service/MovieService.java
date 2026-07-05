package com.chalchitraghar.modules.movies.service;

import java.util.List;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieSummaryResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieSummaryResponse;
import com.chalchitraghar.modules.movies.enums.MovieStatus;

/**
 * Service for movie management operations.
 */
public interface MovieService {

    AdminMovieDetailResponse addMovie(MovieRequest dto);

    AdminMovieDetailResponse updateMovie(Long id, MovieRequest dto);

    void deleteMovie(Long id);

    List<PublicMovieSummaryResponse> getPublicMovies();

    PublicMovieDetailResponse getPublicMovieById(Long id);

    List<PublicMovieSummaryResponse> getPublicMoviesByStatus(MovieStatus status);

    List<AdminMovieSummaryResponse> getAdminMovies();

    AdminMovieDetailResponse getAdminMovieById(Long id);
}
