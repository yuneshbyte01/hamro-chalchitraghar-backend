package com.chalchitraghar.modules.tickets.service;
import java.time.LocalDateTime; import com.chalchitraghar.modules.tickets.dto.response.TicketValidationHistoryResponse; import com.chalchitraghar.shared.response.PageResponse;
public interface TicketValidationQueryService { PageResponse<TicketValidationHistoryResponse> search(int page,int size,String ticketReference,String result,Long validatedBy,Long showId,LocalDateTime from,LocalDateTime to,String deviceId,String location); }
