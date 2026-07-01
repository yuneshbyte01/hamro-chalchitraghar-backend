package com.chalchitraghar.applications.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.movies.dto.request.MovieRequest;
import com.chalchitraghar.modules.movies.dto.response.MovieResponse;
import com.chalchitraghar.modules.movies.service.MovieService;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "List movies for admin")
    public ResponseEntity<ApiResponse<List<MovieResponse>>> getAllMovies() {
        return ResponseEntity.ok(ApiResponse.success("Movies fetched successfully", movieService.getAllMovies()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get movie by ID for admin")
    public ResponseEntity<ApiResponse<MovieResponse>> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Movie fetched successfully", movieService.getMovieById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a movie")
    public ResponseEntity<ApiResponse<MovieResponse>> createMovie(@Valid @RequestBody MovieRequest dto) {
        MovieResponse created = movieService.addMovie(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Movie created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a movie")
    public ResponseEntity<ApiResponse<MovieResponse>> updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest dto) {
        return ResponseEntity.ok(ApiResponse.success("Movie updated successfully", movieService.updateMovie(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a movie", description = "Marks the movie as ENDED.")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
