package com.chalchitraghar.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import lombok.RequiredArgsConstructor;
import com.chalchitraghar.dto.seat.SeatResponseDto;
import com.chalchitraghar.service.SeatService;

@RestController
@RequestMapping("/api/shows")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping("/{showId}/seats")
    public ResponseEntity<List<SeatResponseDto>> getAllSeatsForShow(@PathVariable Long showId) {
        return ResponseEntity.ok(seatService.getAllSeatsForShow(showId));
    }

}
