package com.chalchitraghar.modules.tickets.dto.request;
import java.time.*;
public record TicketSearchCriteria(String search,String status,Long showId,Long movieId,Long hallId,String bookingReference,
 Long customerId,LocalDate showDateFrom,LocalDate showDateTo,LocalDateTime checkedInFrom,LocalDateTime checkedInTo,
 LocalDateTime issuedFrom,LocalDateTime issuedTo,Long revokedBy,Long checkedInBy) {}
