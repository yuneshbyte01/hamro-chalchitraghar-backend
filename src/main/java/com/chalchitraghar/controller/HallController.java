package com.chalchitraghar.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.chalchitraghar.dto.hall.HallResponse;
import com.chalchitraghar.service.HallService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for public hall information endpoints.
 */
@RestController
@RequestMapping("/api/halls")
@RequiredArgsConstructor
public class HallController {

    private final HallService hallService;

    /**
     * Retrieves all halls.
     *
     * @return list of all hall responses
     */
    @GetMapping
    public ResponseEntity<List<HallResponse>> getAllHalls() {
        List<HallResponse> halls = hallService.getAllHalls();
        return ResponseEntity.ok(halls);
    }

    /**
     * Retrieves a hall by ID.
     *
     * @param id the hall ID
     * @return hall response
     */
    @GetMapping("/{id}")
    public ResponseEntity<HallResponse> getHallById(@PathVariable Long id) {
        HallResponse hall = hallService.getHallById(id);
        return ResponseEntity.ok(hall);
    }

    /**
     * Retrieves all active halls.
     *
     * @return list of active hall responses
     */
    @GetMapping("/active")
    public ResponseEntity<List<HallResponse>> getActiveHalls() {
        List<HallResponse> halls = hallService.getActiveHalls();
        return ResponseEntity.ok(halls);
    }

}
