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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for public show information. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/public/shows")
@RequiredArgsConstructor
@Tag(name = "Public Shows", description = "Public show browsing endpoints")
public class ShowController {

    private final ShowService showService;

    @GetMapping
    @Operation(summary = "List public shows")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getAllShows() {
        return ResponseEntity.ok(ApiResponse.success("Shows fetched successfully", showService.getAllShows()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a public show by ID")
    public ResponseEntity<ApiResponse<ShowResponse>> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Show fetched successfully", showService.getShowById(id)));
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "List shows for a movie")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsByMovie(@PathVariable Long movieId) {
        return ResponseEntity.ok(ApiResponse.success("Movie shows fetched successfully", showService.getShowsByMovie(movieId)));
    }

    @GetMapping(params = {"movieId", "date"})
    @Operation(summary = "List shows for a movie and date")
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getShowsByMovieAndDate(
            @RequestParam Long movieId,
            @RequestParam LocalDate date) {
        return ResponseEntity.ok(ApiResponse.success(
                "Movie shows fetched successfully",
                showService.getShowsByMovieAndShowDate(movieId, date)));
    }
}
