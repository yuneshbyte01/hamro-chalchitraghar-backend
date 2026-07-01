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

import com.chalchitraghar.modules.shows.dto.request.ShowRequest;
import com.chalchitraghar.modules.shows.dto.response.ShowResponse;
import com.chalchitraghar.modules.shows.service.ShowService;
import com.chalchitraghar.shared.response.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin show management. CRUD operations. Admin only.
 */
@RestController
@RequestMapping("/api/admin/shows")
@RequiredArgsConstructor
public class AdminShowController {

    private final ShowService showService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<ShowResponse>>> getAllShows() {
        return ResponseEntity.ok(ApiResponse.success("Shows fetched successfully", showService.getAllShows()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowResponse>> getShowById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Show fetched successfully", showService.getShowById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ShowResponse>> createShow(@Valid @RequestBody ShowRequest dto) {
        ShowResponse created = showService.addShow(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Show created successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ShowResponse>> updateShow(@PathVariable Long id, @Valid @RequestBody ShowRequest dto) {
        return ResponseEntity.ok(ApiResponse.success("Show updated successfully", showService.updateShow(id, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShow(@PathVariable Long id) {
        showService.deleteShow(id);
        return ResponseEntity.noContent().build();
    }
}
