package com.chalchitraghar.applications.publicapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.halls.dto.response.PublicHallDetailResponse;
import com.chalchitraghar.modules.halls.dto.response.PublicHallSummaryResponse;
import com.chalchitraghar.modules.halls.service.HallService;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for public hall information. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/public/halls")
@RequiredArgsConstructor
@Tag(name = "Public Halls", description = "Public hall browsing endpoints")
public class HallController {

    private final HallService hallService;

    @GetMapping
    @Operation(summary = "List active public halls", description = "Returns only ACTIVE halls using the public hall summary contract.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active public halls fetched",
            content = @Content(schema = @Schema(implementation = PublicHallSummaryResponse.class)))
    public ResponseEntity<ApiResponse<List<PublicHallSummaryResponse>>> getAllHalls() {
        return ResponseEntity.ok(ApiResponse.success("Halls fetched successfully", hallService.getPublicHalls()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active public hall by ID", description = "Inactive halls are hidden from public detail lookup and return 404.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active public hall fetched",
            content = @Content(schema = @Schema(implementation = PublicHallDetailResponse.class)))
    public ResponseEntity<ApiResponse<PublicHallDetailResponse>> getHallById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Hall fetched successfully", hallService.getPublicHallById(id)));
    }

    @GetMapping("/active")
    @Operation(summary = "List active public halls")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Active public halls fetched",
            content = @Content(schema = @Schema(implementation = PublicHallSummaryResponse.class)))
    public ResponseEntity<ApiResponse<List<PublicHallSummaryResponse>>> getActiveHalls() {
        return ResponseEntity.ok(ApiResponse.success("Active halls fetched successfully", hallService.getPublicActiveHalls()));
    }
}
