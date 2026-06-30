package com.chalchitraghar.applications.publicapi;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.shows.dto.response.ShowResponse;
import com.chalchitraghar.modules.shows.service.ShowService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public show information. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public ResponseEntity<List<ShowResponse>> getAllShows() {
        return ResponseEntity.ok(showService.getAllShows());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(showService.getShowById(id));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(showService.getShowsByMovie(movieId));
    }

    @GetMapping(params = {"movieId", "date"})
    public ResponseEntity<List<ShowResponse>> getShowsByMovieAndDate(
            @RequestParam Long movieId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(showService.getShowsByMovieAndShowDate(movieId, date));
    }
}
