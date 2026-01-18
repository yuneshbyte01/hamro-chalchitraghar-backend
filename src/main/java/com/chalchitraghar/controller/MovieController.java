package com.chalchitraghar.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.movie.MovieResponseDto;
import com.chalchitraghar.model.enums.MovieStatus;
import com.chalchitraghar.service.MovieService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/movies")
@RequiredArgsConstructor
public class MovieController {

    private final MovieService movieService;

    @GetMapping("/test")
    public ResponseEntity<String> test() {
        return ResponseEntity.ok("Working....");
    }

    @GetMapping
    public ResponseEntity<List<MovieResponseDto>> getAllMovies() {
        List<MovieResponseDto> movies = movieService.getAllMovies();
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovieResponseDto> getMovieById(@PathVariable Long id) {
        MovieResponseDto movie = movieService.getMovieById(id);
        return ResponseEntity.ok(movie);
    }

    @GetMapping("/now-showing")
    public ResponseEntity<List<MovieResponseDto>> getNowShowingMovies() {
        List<MovieResponseDto> movies = movieService.getMoviesByStatus(MovieStatus.NOW_SHOWING);
        return ResponseEntity.ok(movies);
    }

    @GetMapping("/upcoming")
    public ResponseEntity<List<MovieResponseDto>> getUpcomingMovies() {
        List<MovieResponseDto> movies = movieService.getMoviesByStatus(MovieStatus.UPCOMING);
        return ResponseEntity.ok(movies);
    }
}
