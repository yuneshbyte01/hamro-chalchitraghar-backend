package com.chalchitraghar.applications.publicapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.movies.dto.response.MovieResponse;
import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.movies.service.MovieService;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "List public movies")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMovies() {
        return ResponseEntity.ok(ApiResponse.success("Movies fetched successfully", movieService.getAllMovies()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a public movie by ID")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Movie fetched successfully", movieService.getMovieById(id)));
    }

    @GetMapping("/now-showing")
    @Operation(summary = "List now showing movies")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getNowShowingMovies() {
        return ResponseEntity.ok(ApiResponse.success(
                "Now showing movies fetched successfully",
                movieService.getMoviesByStatus(MovieStatus.NOW_SHOWING)));
    }

    @GetMapping("/upcoming")
    @Operation(summary = "List upcoming movies")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getUpcomingMovies() {
        return ResponseEntity.ok(ApiResponse.success(
                "Upcoming movies fetched successfully",
                movieService.getMoviesByStatus(MovieStatus.UPCOMING)));
    }
}
