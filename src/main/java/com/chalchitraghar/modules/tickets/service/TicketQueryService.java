package com.chalchitraghar.modules.tickets.service;

import com.chalchitraghar.modules.tickets.dto.request.TicketSearchCriteria;
import com.chalchitraghar.modules.tickets.dto.response.*;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.response.PageResponse;

public interface TicketQueryService {
    PageResponse<CustomerTicketSummaryResponse> customer(
            User u, TicketSearchCriteria c, int p, int s, String sort, String dir);

    PageResponse<StaffTicketSummaryResponse> staff(
            TicketSearchCriteria c, int p, int s, String sort, String dir);

    PageResponse<AdminTicketSummaryResponse> admin(
            TicketSearchCriteria c, int p, int s, String sort, String dir);

    TicketMetricsResponse metrics(TicketSearchCriteria c);

    java.util.Map<String, Long> validationSummary();

    java.util.List<TicketConsistencyIssue> inconsistencies();
}
