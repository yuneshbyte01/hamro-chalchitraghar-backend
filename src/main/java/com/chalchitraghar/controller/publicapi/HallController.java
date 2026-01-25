package com.chalchitraghar.controller.publicapi;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.dto.hall.HallResponse;
import com.chalchitraghar.service.HallService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public hall information. Read-only. No authentication required.
 */
@RestController
@RequestMapping("/api/halls")
@RequiredArgsConstructor
public class HallController {

    private final HallService hallService;

    @GetMapping
    public ResponseEntity<List<HallResponse>> getAllHalls() {
        return ResponseEntity.ok(hallService.getAllHalls());
    }

    @GetMapping("/{id}")
    public ResponseEntity<HallResponse> getHallById(@PathVariable Long id) {
        return ResponseEntity.ok(hallService.getHallById(id));
    }

    @GetMapping("/active")
    public ResponseEntity<List<HallResponse>> getActiveHalls() {
        return ResponseEntity.ok(hallService.getActiveHalls());
    }
}
