package com.chalchitraghar.modules.tickets.service;

import com.chalchitraghar.modules.tickets.dto.response.TicketValidationHistoryResponse;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.LocalDateTime;

public interface TicketValidationQueryService {
    PageResponse<TicketValidationHistoryResponse> search(
            int page,
            int size,
            String ticketReference,
            String result,
            Long validatedBy,
            Long showId,
            LocalDateTime from,
            LocalDateTime to,
            String deviceId,
            String location);
}
