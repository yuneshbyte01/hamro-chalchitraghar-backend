package com.chalchitraghar.modules.halls.service;

import com.chalchitraghar.modules.halls.dto.response.AdminSeatLayoutResponse;

/**
 * Service for generating seat layout templates for halls.
 */
public interface SeatLayoutService {

    void generateSeatTemplates(Long hallId);

    AdminSeatLayoutResponse getSeatLayout(Long hallId);
}
