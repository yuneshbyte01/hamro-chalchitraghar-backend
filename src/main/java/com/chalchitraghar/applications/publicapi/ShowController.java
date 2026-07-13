package com.chalchitraghar.applications.publicapi;

import com.chalchitraghar.modules.shows.dto.request.ShowSearchCriteria;
import com.chalchitraghar.modules.shows.dto.response.PublicShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.PublicShowSummaryResponse;
import com.chalchitraghar.modules.shows.service.ShowService;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for public show information. Read-only. No authentication required. */
@RestController
@RequestMapping("/api/public/shows")
@RequiredArgsConstructor
@Tag(name = "Public Shows", description = "Public show browsing endpoints")
public class ShowController {

    private final ShowService showService;

    @GetMapping
    @Operation(
            summary = "List public shows",
            description =
                    "Returns paginated visible shows; search matches movie title and hall name.")
    public ResponseEntity<ApiResponse<PageResponse<PublicShowSummaryResponse>>> getAllShows(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0")
                    int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(
                            description =
                                    "Sort field: showDate, showTime, endTime, status, createdAt, updatedAt")
                    @RequestParam(defaultValue = "showDate")
                    String sortBy,
            @Parameter(description = "Sort direction: asc or desc")
                    @RequestParam(defaultValue = "asc")
                    String sortDir,
            @Parameter(description = "Case-insensitive movie title or hall name search")
                    @RequestParam(required = false)
                    String search,
            @Parameter(description = "Filter by movie ID") @RequestParam(required = false)
                    Long movieId,
            @Parameter(description = "Filter by hall ID") @RequestParam(required = false)
                    Long hallId,
            @Parameter(description = "Filter by show date")
                    @RequestParam(required = false)
                    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate showDate) {
        var criteria = new ShowSearchCriteria(search, movieId, hallId, null, showDate);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Shows fetched successfully",
                        showService.getPublicShows(criteria, page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a public show by ID")
    public ResponseEntity<ApiResponse<PublicShowDetailResponse>> getShowById(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Show fetched successfully", showService.getPublicShowById(id)));
    }

    @GetMapping("/movie/{movieId}")
    @Operation(summary = "List shows for a movie")
    public ResponseEntity<ApiResponse<List<PublicShowSummaryResponse>>> getShowsByMovie(
            @PathVariable Long movieId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Movie shows fetched successfully",
                        showService.getPublicShowsByMovie(movieId)));
    }

    @GetMapping(params = {"movieId", "date"})
    @Operation(summary = "List shows for a movie and date")
    public ResponseEntity<ApiResponse<List<PublicShowSummaryResponse>>> getShowsByMovieAndDate(
            @RequestParam Long movieId, @RequestParam LocalDate date) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Movie shows fetched successfully",
                        showService.getPublicShowsByMovieAndShowDate(movieId, date)));
    }
}
