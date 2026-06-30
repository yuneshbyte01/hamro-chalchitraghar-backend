package com.chalchitraghar.applications.publicapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import com.chalchitraghar.modules.seats.service.SeatService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public seat availability. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<SeatResponse>> getAllSeatsForShow(@PathVariable Long showId) {
        return ResponseEntity.ok(seatService.getAllSeatsForShow(showId));
    }
}
