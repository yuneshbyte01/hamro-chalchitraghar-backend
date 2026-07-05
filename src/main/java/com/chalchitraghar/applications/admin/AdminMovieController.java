package com.chalchitraghar.applications.admin;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.request.MovieSearchCriteria;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieSummaryResponse;
import com.chalchitraghar.modules.movies.service.MovieService;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin movie management. CRUD operations. Admin only.
 */
@RestController
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
@Tag(name = "Admin Movies", description = "Admin movie management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminMovieController {

    private final MovieService movieService;

    @GetMapping
    @Operation(summary = "List movies for admin", description = "Returns paginated admin movie summaries.")
    public ResponseEntity<ApiResponse<PageResponse<AdminMovieSummaryResponse>>> getAllMovies(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field: id, title, genre, language, releaseDate, status, createdAt, updatedAt, durationMinutes")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortDir,
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
        PageResponse<AdminMovieSummaryResponse> movies = movieService.getAdminMovies(
                new MovieSearchCriteria(search, status, genre, language, releaseDateFrom, releaseDateTo),
                page,
                size,
                sortBy,
                sortDir);
        return ResponseEntity.ok(ApiResponse.success("Movies fetched successfully", movies));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID for admin")
    public ResponseEntity<ApiResponse<AdminMovieDetailResponse>> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Movie fetched successfully", movieService.getAdminMovieById(id)));
    }

    @PostMapping
    @Operation(
            summary = "Create a movie",
            description = "Creates a movie after validating duplicate title/release date, duration, poster URL, release date consistency, and status.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MOVIE_REQUEST_EXAMPLE)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation failure",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = VALIDATION_ERROR_EXAMPLE)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Duplicate movie or release date/status business rule conflict",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MOVIE_CONFLICT_EXAMPLE)))
    public ResponseEntity<ApiResponse<AdminMovieDetailResponse>> createMovie(@Valid @RequestBody MovieRequest dto) {
        AdminMovieDetailResponse created = movieService.addMovie(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Movie created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a movie",
            description = "Updates a movie after validating duplicate title/release date, status transition, duration, poster URL, and release date consistency.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MOVIE_REQUEST_EXAMPLE)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "Validation failure",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = VALIDATION_ERROR_EXAMPLE)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Duplicate movie, invalid status transition, or release date/status business rule conflict",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = MOVIE_CONFLICT_EXAMPLE)))
    public ResponseEntity<ApiResponse<AdminMovieDetailResponse>> updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest dto) {
        return ResponseEntity.ok(ApiResponse.success("Movie updated successfully", movieService.updateMovie(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Soft-delete a movie",
            description = "Marks the movie as ENDED when no future active SCHEDULED or RUNNING shows exist. Movie rows are not physically deleted.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Movie has future active shows",
            content = @Content(mediaType = "application/json", examples = @ExampleObject(value = FUTURE_SHOW_CONFLICT_EXAMPLE)))
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }

    private static final String MOVIE_REQUEST_EXAMPLE = """
            {
              "title": "Jatra",
              "genre": "Comedy",
              "durationMinutes": 125,
              "language": "Nepali",
              "description": "A Nepali comedy movie about an unexpected chain of events.",
              "posterUrl": "https://example.com/posters/jatra.jpg",
              "releaseDate": "2026-07-05",
              "status": "NOW_SHOWING"
            }
            """;

    private static final String VALIDATION_ERROR_EXAMPLE = """
            {
              "success": false,
              "message": "Validation failed",
              "data": null,
              "errors": ["durationMinutes: Duration must be at least 1 minute"]
            }
            """;

    private static final String MOVIE_CONFLICT_EXAMPLE = """
            {
              "success": false,
              "message": "Movie already exists with the same title and release date",
              "data": null,
              "errors": []
            }
            """;

    private static final String FUTURE_SHOW_CONFLICT_EXAMPLE = """
            {
              "success": false,
              "message": "Cannot end movie while future active shows exist",
              "data": null,
              "errors": []
            }
            """;
}
