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

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public hall information. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/public/halls")
@RequiredArgsConstructor
public class HallController {

    private final HallService hallService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HallResponse>>> getAllHalls() {
        return ResponseEntity.ok(ApiResponse.success("Halls fetched successfully", hallService.getAllHalls()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HallResponse>> getHallById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Hall fetched successfully", hallService.getHallById(id)));
    }

    @GetMapping("/active")
    public ResponseEntity<ApiResponse<List<HallResponse>>> getActiveHalls() {
        return ResponseEntity.ok(ApiResponse.success("Active halls fetched successfully", hallService.getActiveHalls()));
    }
}
