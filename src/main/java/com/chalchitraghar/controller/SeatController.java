package com.chalchitraghar.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import com.chalchitraghar.dto.seat.SeatResponse;
import com.chalchitraghar.service.SeatService;

/**
 * REST controller for seat information endpoints.
 */
@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    /**
     * Retrieves all seats for a specific show, ordered by position index.
     *
     * @param showId the show ID
     * @return list of seat responses for the show
     */
    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<SeatResponse>> getAllSeatsForShow(@PathVariable Long showId) {
        return ResponseEntity.ok(seatService.getAllSeatsForShow(showId));
    }

}
