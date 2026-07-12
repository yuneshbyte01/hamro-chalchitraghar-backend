package com.chalchitraghar.modules.tickets.service;
import com.chalchitraghar.modules.tickets.dto.request.TicketScanRequest;
import com.chalchitraghar.modules.tickets.dto.response.TicketScanResponse;
import com.chalchitraghar.modules.users.entity.User;
public interface TicketValidationService {
 TicketScanResponse scan(TicketScanRequest request,User staff,String deviceId,String location,String requestId);
}
