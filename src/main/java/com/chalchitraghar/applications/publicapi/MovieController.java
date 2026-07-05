package com.chalchitraghar.applications.publicapi;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.movies.dto.request.MovieSearchCriteria;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieSummaryResponse;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.movies.service.MovieService;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for public movie information. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/public/movies")
@RequiredArgsConstructor
@Tag(name = "Public Movies", description = "Public movie browsing endpoints")
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    @Operation(summary = "List public movies", description = "Returns paginated public movie summaries.")
    public ResponseEntity<ApiResponse<PageResponse<PublicMovieSummaryResponse>>> getAllMovies(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field: id, title, genre, language, releaseDate, status, createdAt, updatedAt, durationMinutes")
            @RequestParam(defaultValue = "releaseDate") String sortBy,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Case-insensitive search term matched against title, genre, and language")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status: UPCOMING, NOW_SHOWING, ENDED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by genre")
            @RequestParam(required = false) String genre,
            @Parameter(description = "Filter by language")
            @RequestParam(required = false) String language,
            @Parameter(description = "Filter movies released on or after this date")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDateFrom,
            @Parameter(description = "Filter movies released on or before this date")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate releaseDateTo) {
        PageResponse<PublicMovieSummaryResponse> movies = movieService.getPublicMovies(
                new MovieSearchCriteria(search, status, genre, language, releaseDateFrom, releaseDateTo),
                page,
                size,
                sortBy,
                sortDir);
        return ResponseEntity.ok(ApiResponse.success("Movies fetched successfully", movies));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a public movie by ID")
    public ResponseEntity<ApiResponse<PublicMovieDetailResponse>> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Movie fetched successfully", movieService.getPublicMovieById(id)));
    }

    @GetMapping("/now-showing")
    @Operation(summary = "List now showing movies")
    public ResponseEntity<ApiResponse<List<PublicMovieSummaryResponse>>> getNowShowingMovies() {
        return ResponseEntity.ok(ApiResponse.success(
                "Now showing movies fetched successfully",
                movieService.getPublicMoviesByStatus(MovieStatus.NOW_SHOWING)));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "List upcoming movies")
    public ResponseEntity<ApiResponse<List<PublicMovieSummaryResponse>>> getUpcomingMovies() {
        return ResponseEntity.ok(ApiResponse.success(
                "Upcoming movies fetched successfully",
                movieService.getPublicMoviesByStatus(MovieStatus.UPCOMING)));
    }
}
