package com.chalchitraghar.applications.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.halls.service.SeatLayoutService;

import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin seat layout management. Admin only.
 */
@RestController
@RequestMapping("/api/admin/halls")
@RequiredArgsConstructor
public class AdminSeatLayoutController {

    private final SeatLayoutService seatLayoutService;

    /**
     * Generates seat layout templates for a hall.
     *
     * @param hallId the hall ID
     * @return success response
     */
    @PostMapping("/{hallId}/seat-layout")
    public ResponseEntity<Void> generateSeatLayout(@PathVariable Long hallId) {
        seatLayoutService.generateSeatTemplates(hallId);
        return ResponseEntity.ok().build();
    }
}
