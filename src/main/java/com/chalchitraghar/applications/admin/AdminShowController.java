package com.chalchitraghar.applications.admin;

import java.time.LocalDate;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import com.chalchitraghar.modules.shows.dto.request.ShowRequest;
import com.chalchitraghar.modules.shows.dto.request.ShowSearchCriteria;
import com.chalchitraghar.modules.shows.dto.request.ShowStatusUpdateRequest;
import com.chalchitraghar.modules.shows.dto.response.AdminShowDetailResponse;
import com.chalchitraghar.modules.shows.dto.response.AdminShowSummaryResponse;
import com.chalchitraghar.modules.shows.service.ShowService;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin show management. CRUD operations. Admin only.
 */
@RestController
@RequestMapping("/api/admin/shows")
@RequiredArgsConstructor
@Tag(name = "Admin Shows", description = "Admin show management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminShowController {

    private final ShowService showService;

    @GetMapping
    @Operation(summary = "List shows for admin", description = "Returns paginated shows of every status; search matches movie title and hall name.")
    public ResponseEntity<ApiResponse<PageResponse<AdminShowSummaryResponse>>> getAllShows(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field: showDate, showTime, endTime, status, createdAt, updatedAt")
            @RequestParam(defaultValue = "showDate") String sortBy,
            @Parameter(description = "Sort direction: asc or desc") @RequestParam(defaultValue = "asc") String sortDir,
            @Parameter(description = "Case-insensitive movie title or hall name search") @RequestParam(required = false) String search,
            @Parameter(description = "Filter by movie ID") @RequestParam(required = false) Long movieId,
            @Parameter(description = "Filter by hall ID") @RequestParam(required = false) Long hallId,
            @Parameter(description = "Filter by status: SCHEDULED, RUNNING, COMPLETED, CANCELLED")
            @RequestParam(required = false) String status,
            @Parameter(description = "Filter by show date") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate showDate) {
        var criteria = new ShowSearchCriteria(search, movieId, hallId, status, showDate);
        return ResponseEntity.ok(ApiResponse.success("Shows fetched successfully",
                showService.getAdminShows(criteria, page, size, sortBy, sortDir)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get show by ID for admin")
    public ResponseEntity<ApiResponse<AdminShowDetailResponse>> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Show fetched successfully", showService.getAdminShowById(id)));
    }

    @PostMapping
    @Operation(summary = "Create a show", description = "Creates a show and generates seats from hall seat templates.")
    public ResponseEntity<ApiResponse<AdminShowDetailResponse>> createShow(@Valid @RequestBody ShowRequest dto) {
        AdminShowDetailResponse created = showService.addShow(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Show created successfully", created));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a show")
    public ResponseEntity<ApiResponse<AdminShowDetailResponse>> updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequest dto) {
        return ResponseEntity.ok(ApiResponse.success("Show updated successfully", showService.updateShow(id, dto)));
    }

    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Update show status",
            description = "Transitions SCHEDULED to RUNNING/CANCELLED or RUNNING to COMPLETED/CANCELLED. Terminal shows cannot transition.")
    public ResponseEntity<ApiResponse<AdminShowDetailResponse>> updateShowStatus(
            @PathVariable Long id,
            @Valid @RequestBody ShowStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Show status updated successfully",
                showService.updateShowStatus(id, request.getStatus())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a show", description = "Marks the show as CANCELLED.")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}
