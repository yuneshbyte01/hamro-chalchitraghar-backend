package com.chalchitraghar.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.show.ShowResponse;
import com.chalchitraghar.service.ShowService;

import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public ResponseEntity<List<ShowResponse>> getAllShows() {
        List<ShowResponse> shows = showService.getAllShows();
        return ResponseEntity.ok(shows);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable Long id) {
        ShowResponse show = showService.getShowById(id);
        return ResponseEntity.ok(show);
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(@PathVariable Long movieId) {
        List<ShowResponse> shows = showService.getShowsByMovie(movieId);
        return ResponseEntity.ok(shows);
    }

    @GetMapping(params = {"movieId", "date"})
    public ResponseEntity<List<ShowResponse>> getShowsByMovieAndDate(
            @RequestParam Long movieId, 
            @RequestParam LocalDate date) {
        List<ShowResponse> shows = showService.getShowsByMovieAndShowDate(movieId, date);
        return ResponseEntity.ok(shows);
    }
}
