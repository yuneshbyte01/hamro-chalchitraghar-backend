package com.chalchitraghar.applications.publicapi;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for health check. Public. No authentication required.
 */
@RestController
@RequestMapping("/api/public/health")
@Tag(name = "Health", description = "Public health check endpoint")
public class HealthController {

    @GetMapping
    @Operation(summary = "Health check")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        return ResponseEntity.ok(ApiResponse.success("Health check successful", Map.of("status", "UP")));
    }
}
