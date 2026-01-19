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

/**
 * REST controller for public show information endpoints.
 */
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    /**
     * Retrieves all shows.
     *
     * @return list of all show responses
     */
    @GetMapping
    public ResponseEntity<List<ShowResponse>> getAllShows() {
        List<ShowResponse> shows = showService.getAllShows();
        return ResponseEntity.ok(shows);
    }

    /**
     * Retrieves a show by ID.
     *
     * @param id the show ID
     * @return show response
     */
    @GetMapping("/{id}")
    public ResponseEntity<ShowResponse> getShowById(@PathVariable Long id) {
        ShowResponse show = showService.getShowById(id);
        return ResponseEntity.ok(show);
    }

    /**
     * Retrieves all shows for a specific movie.
     *
     * @param movieId the movie ID
     * @return list of show responses for the movie
     */
    @GetMapping("/movie/{movieId}")
    public ResponseEntity<List<ShowResponse>> getShowsByMovie(@PathVariable Long movieId) {
        List<ShowResponse> shows = showService.getShowsByMovie(movieId);
        return ResponseEntity.ok(shows);
    }

    /**
     * Retrieves shows for a specific movie on a specific date.
     *
     * @param movieId the movie ID
     * @param date the show date
     * @return list of show responses matching the criteria
     */
    @GetMapping(params = {"movieId", "date"})
    public ResponseEntity<List<ShowResponse>> getShowsByMovieAndDate(
            @RequestParam Long movieId, 
            @RequestParam LocalDate date) {
        List<ShowResponse> shows = showService.getShowsByMovieAndShowDate(movieId, date);
        return ResponseEntity.ok(shows);
    }
}
