package com.chalchitraghar.modules.movies.service;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.request.MovieSearchCriteria;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieSummaryResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieSummaryResponse;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.shared.response.PageResponse;
import java.util.List;

/** Service for movie management operations. */
public interface MovieService {

    AdminMovieDetailResponse addMovie(MovieRequest dto);

    AdminMovieDetailResponse updateMovie(Long id, MovieRequest dto);

    void deleteMovie(Long id);

    PageResponse<PublicMovieSummaryResponse> getPublicMovies(
            MovieSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    PublicMovieDetailResponse getPublicMovieById(Long id);

    List<PublicMovieSummaryResponse> getPublicMoviesByStatus(MovieStatus status);

    PageResponse<AdminMovieSummaryResponse> getAdminMovies(
            MovieSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    AdminMovieDetailResponse getAdminMovieById(Long id);
}
