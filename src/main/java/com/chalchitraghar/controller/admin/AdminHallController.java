package com.chalchitraghar.controller.admin;

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

import com.chalchitraghar.dto.hall.HallRequest;
import com.chalchitraghar.dto.hall.HallResponse;
import com.chalchitraghar.service.HallService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin hall management. CRUD operations. Admin only.
 */
@RestController
@RequestMapping("/api/admin/halls")
@RequiredArgsConstructor
public class AdminHallController {

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

    @PostMapping
    public ResponseEntity<HallResponse> createHall(@Valid @RequestBody HallRequest dto) {
        HallResponse hall = hallService.addHall(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(hall);
    }

    @PutMapping("/{id}")
    public ResponseEntity<HallResponse> updateHall(@PathVariable Long id, @Valid @RequestBody HallRequest dto) {
        return ResponseEntity.ok(hallService.updateHall(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteHall(@PathVariable Long id) {
        hallService.deleteHall(id);
        return ResponseEntity.noContent().build();
    }
}
