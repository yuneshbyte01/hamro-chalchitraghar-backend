package com.chalchitraghar.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.movie.MovieResponse;
import com.chalchitraghar.model.enums.MovieStatus;
import com.chalchitraghar.service.MovieService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public movie information endpoints.
 */
@RestController
@RequestMapping("/api/movies")
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
     * Retrieves all movies currently showing.
     *
     * @return list of now-showing movie responses
     */
    @GetMapping("/now-showing")
    public ResponseEntity<List<MovieResponse>> getNowShowingMovies() {
        List<MovieResponse> movies = movieService.getMoviesByStatus(MovieStatus.NOW_SHOWING);
        return ResponseEntity.ok(movies);
    }

    /**
     * Retrieves all upcoming movies.
     *
     * @return list of upcoming movie responses
     */
    @GetMapping("/upcoming")
    public ResponseEntity<List<MovieResponse>> getUpcomingMovies() {
        List<MovieResponse> movies = movieService.getMoviesByStatus(MovieStatus.UPCOMING);
        return ResponseEntity.ok(movies);
    }
}
