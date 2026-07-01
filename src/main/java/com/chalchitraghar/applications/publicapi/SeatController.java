package com.chalchitraghar.applications.publicapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import com.chalchitraghar.modules.seats.service.SeatService;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for public seat availability. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/public/shows")
@RequiredArgsConstructor
@Tag(name = "Public Shows", description = "Public show seat browsing endpoints")
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{showId}/seats")
    @Operation(summary = "List seats for a show")
    public ResponseEntity<ApiResponse<List<SeatResponse>>> getAllSeatsForShow(@PathVariable Long showId) {
        return ResponseEntity.ok(ApiResponse.success("Seats fetched successfully", seatService.getAllSeatsForShow(showId)));
    }
}
