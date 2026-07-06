package com.chalchitraghar.applications.admin;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.halls.dto.request.HallRequest;
import com.chalchitraghar.modules.halls.dto.request.HallSearchCriteria;
import com.chalchitraghar.modules.halls.dto.response.AdminHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.AdminHallSummaryResponse;
import com.chalchitraghar.modules.halls.service.HallService;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.response.PageResponse;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin hall management. CRUD operations. Admin only.
 */
@RestController
@RequestMapping("/api/admin/halls")
@RequiredArgsConstructor
@Tag(name = "Admin Halls", description = "Admin hall management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class AdminHallController {

    private final HallService hallService;

    @GetMapping
    @Operation(
            summary = "List halls for admin",
            description = "Returns paginated admin hall summaries. Search matches name and layoutRef case-insensitively.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin halls fetched",
            content = @Content(schema = @Schema(implementation = AdminHallSummaryResponse.class)))
    public ResponseEntity<ApiResponse<PageResponse<AdminHallSummaryResponse>>> getAllHalls(
            @Parameter(description = "Zero-based page index")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Sort field: id, name, capacity, layoutRef, status, createdAt, updatedAt")
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Sort direction: asc or desc")
            @RequestParam(defaultValue = "desc") String sortDir,
            @Parameter(description = "Case-insensitive search term matched against hall name and layoutRef")
            @RequestParam(required = false) String search,
            @Parameter(description = "Filter by status: ACTIVE, INACTIVE")
            @RequestParam(required = false) String status) {
        PageResponse<AdminHallSummaryResponse> halls = hallService.getAdminHalls(
                new HallSearchCriteria(search, status),
                page,
                size,
                sortBy,
                sortDir);
        return ResponseEntity.ok(ApiResponse.success("Halls fetched successfully", halls));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hall by ID for admin")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Admin hall fetched",
            content = @Content(schema = @Schema(implementation = AdminHallDetailResponse.class)))
    public ResponseEntity<ApiResponse<AdminHallDetailResponse>> getHallById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Hall fetched successfully", hallService.getAdminHallById(id)));
    }

    @GetMapping("/active")
    @Operation(summary = "List active halls for admin")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active admin halls fetched",
            content = @Content(schema = @Schema(implementation = AdminHallSummaryResponse.class)))
    public ResponseEntity<ApiResponse<List<AdminHallSummaryResponse>>> getActiveHalls() {
        return ResponseEntity.ok(ApiResponse.success("Active halls fetched successfully", hallService.getAdminActiveHalls()));
    }

    @PostMapping
    @Operation(summary = "Create a hall", description = "Creates a hall after validating unique name, capacity, layout reference, and status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Hall created",
            content = @Content(schema = @Schema(implementation = AdminHallDetailResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Duplicate hall name")
    public ResponseEntity<ApiResponse<AdminHallDetailResponse>> createHall(@Valid @RequestBody HallRequest dto) {
        AdminHallDetailResponse hall = hallService.addHall(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hall created successfully", hall));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Update a hall",
            description = """
                    Updates a hall after validating unique name, capacity, layout reference, and ACTIVE/INACTIVE lifecycle.
                    Inactivating an ACTIVE hall is rejected when future active SCHEDULED or RUNNING shows exist.
                    Once seat templates exist, capacity and layoutRef cannot be changed.""")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Hall updated",
            content = @Content(schema = @Schema(implementation = AdminHallDetailResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Duplicate hall name, invalid lifecycle transition, future active shows exist, or seat template dependency blocks capacity/layoutRef changes")
    public ResponseEntity<ApiResponse<AdminHallDetailResponse>> updateHall(@PathVariable Long id, @Valid @RequestBody HallRequest dto) {
        return ResponseEntity.ok(ApiResponse.success("Hall updated successfully", hallService.updateHall(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Soft-delete a hall",
            description = "Marks the hall as INACTIVE when no future active SCHEDULED or RUNNING shows exist. Hall rows are not physically deleted.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Hall marked inactive")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Hall not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Hall has future active shows")
    public ResponseEntity<Void> deleteHall(@PathVariable Long id) {
        hallService.deleteHall(id);
        return ResponseEntity.noContent().build();
    }
}
