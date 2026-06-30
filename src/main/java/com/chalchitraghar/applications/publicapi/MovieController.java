package com.chalchitraghar.applications.publicapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.movies.MovieResponse;
import com.chalchitraghar.modules.movies.MovieStatus;
import com.chalchitraghar.modules.movies.MovieService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public movie information. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping
    public ResponseEntity<List<MovieResponse>> getAllMovies() {
        return ResponseEntity.ok(movieService.getAllMovies());
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponse> getMovieById(@PathVariable Long id) {
        return ResponseEntity.ok(movieService.getMovieById(id));
    }

    @GetMapping("/now-showing")
    public ResponseEntity<List<MovieResponse>> getNowShowingMovies() {
        return ResponseEntity.ok(movieService.getMoviesByStatus(MovieStatus.NOW_SHOWING));
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<MovieResponse>> getUpcomingMovies() {
        return ResponseEntity.ok(movieService.getMoviesByStatus(MovieStatus.UPCOMING));
    }
}
