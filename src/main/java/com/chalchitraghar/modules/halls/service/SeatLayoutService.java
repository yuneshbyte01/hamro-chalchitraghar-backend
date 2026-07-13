package com.chalchitraghar.modules.halls.service;

import com.chalchitraghar.modules.halls.dto.request.SeatTemplateSearchCriteria;
import com.chalchitraghar.modules.halls.dto.response.AdminSeatLayoutResponse;

/** Service for generating seat layout templates for halls. */
public interface SeatLayoutService {

    void generateSeatTemplates(Long hallId);

    AdminSeatLayoutResponse getSeatLayout(Long hallId, SeatTemplateSearchCriteria criteria);

    AdminSeatLayoutResponse regenerateSeatTemplates(Long hallId);
}
