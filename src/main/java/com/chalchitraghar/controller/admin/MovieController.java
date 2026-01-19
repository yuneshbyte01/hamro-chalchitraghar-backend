package com.chalchitraghar.controller.admin;

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

import com.chalchitraghar.dto.movie.MovieRequest;
import com.chalchitraghar.dto.movie.MovieResponse;
import com.chalchitraghar.service.MovieService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin movie management endpoints.
 * Requires ADMIN role for all operations.
 */
@RestController("adminMovieController")
@RequestMapping("/api/admin/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    /**
     * Retrieves all movies.
     *
     * @return list of all movie responses
     */
    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        List<MovieResponse> movies = movieService.getAllMovies();
        return ResponseEntity.ok(movies);
    }

    /**
     * Retrieves a movie by ID.
     *
     * @param id the movie ID
     * @return movie response
     */
    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        MovieResponse movie = movieService.getMovieById(id);
        return ResponseEntity.ok(movie);
    }

    /**
     * Creates a new movie.
     *
     * @param dto movie request containing movie details
     * @return created movie response
     */
    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody MovieRequest dto) {
        MovieResponse createdMovie = movieService.addMovie(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdMovie);
    }

    /**
     * Updates an existing movie.
     *
     * @param id the movie ID
     * @param dto movie request containing updated details
     * @return updated movie response
     */
    @PutMapping("/{id}")
    public ResponseEntity<MovieResponse> updateMovie(@PathVariable Long id, @Valid @RequestBody MovieRequest dto) {
        MovieResponse updatedMovie = movieService.updateMovie(id, dto);
        return ResponseEntity.ok(updatedMovie);
    }

    /**
     * Soft deletes a movie by setting its status to ENDED.
     *
     * @param id the movie ID
     * @return no content response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMovie(@PathVariable Long id) {
        movieService.deleteMovie(id);
        return ResponseEntity.noContent().build();
    }
}
