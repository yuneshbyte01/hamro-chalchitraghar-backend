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
import com.chalchitraghar.shared.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public show information. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/public/shows")
@RequiredArgsConstructor
public class ShowController {

    private final ShowService showService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getAllShows() {
        return ResponseEntity.ok(ApiResponse.success("Shows fetched successfully", showService.getAllShows()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowResponse>> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Show fetched successfully", showService.getShowById(id)));
    }

    @GetMapping("/movie/{movieId}")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.success("Movie shows fetched successfully", showService.getShowsByMovie(movieId)));
    }

    @GetMapping(params = {"movieId", "date"})
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsByMovieAndDate(
            @RequestParam Long movieId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                "Movie shows fetched successfully",
                showService.getShowsByMovieAndShowDate(movieId, date)));
    }
}
