package com.chalchitraghar.applications.publicapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.halls.dto.response.HallResponse;
import com.chalchitraghar.modules.halls.service.HallService;
import com.chalchitraghar.shared.response.ApiResponse;

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
    @Operation(summary = "List public halls")
    public ResponseEntity<ApiResponse<List<HallResponse>>> getAllHalls() {
        return ResponseEntity.ok(ApiResponse.success("Halls fetched successfully", hallService.getAllHalls()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a public hall by ID")
    public ResponseEntity<ApiResponse<HallResponse>> getHallById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Hall fetched successfully", hallService.getHallById(id)));
    }

    @GetMapping("/active")
    @Operation(summary = "List active public halls")
    public ResponseEntity<ApiResponse<List<HallResponse>>> getActiveHalls() {
        return ResponseEntity.ok(ApiResponse.success("Active halls fetched successfully", hallService.getActiveHalls()));
    }
}
