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
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.halls.dto.request.HallRequest;
import com.chalchitraghar.modules.halls.dto.response.HallResponse;
import com.chalchitraghar.modules.halls.service.HallService;
import com.chalchitraghar.shared.response.ApiResponse;

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
    @Operation(summary = "List halls for admin")
    public ResponseEntity<ApiResponse<List<HallResponse>>> getAllHalls() {
        return ResponseEntity.ok(ApiResponse.success("Halls fetched successfully", hallService.getAllHalls()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get hall by ID for admin")
    public ResponseEntity<ApiResponse<HallResponse>> getHallById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Hall fetched successfully", hallService.getHallById(id)));
    }

    @GetMapping("/active")
    @Operation(summary = "List active halls for admin")
    public ResponseEntity<ApiResponse<List<HallResponse>>> getActiveHalls() {
        return ResponseEntity.ok(ApiResponse.success("Active halls fetched successfully", hallService.getActiveHalls()));
    }

    @PostMapping
    @Operation(summary = "Create a hall")
    public ResponseEntity<ApiResponse<HallResponse>> createHall(@Valid @RequestBody HallRequest dto) {
        HallResponse hall = hallService.addHall(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Hall created successfully", hall));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a hall")
    public ResponseEntity<ApiResponse<HallResponse>> updateHall(@PathVariable Long id, @Valid @RequestBody HallRequest dto) {
        return ResponseEntity.ok(ApiResponse.success("Hall updated successfully", hallService.updateHall(id, dto)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a hall", description = "Marks the hall as INACTIVE.")
    public ResponseEntity<Void> deleteHall(@PathVariable Long id) {
        hallService.deleteHall(id);
        return ResponseEntity.noContent().build();
    }
}
