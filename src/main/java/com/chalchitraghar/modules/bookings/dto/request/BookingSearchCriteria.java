package com.chalchitraghar.modules.bookings.dto.request;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Search and filter criteria shared by booking list audiences. */
public record BookingSearchCriteria(
        String search,
        String status,
        Long showId,
        Long movieId,
        Long hallId,
        Long customerId,
        LocalDate showDateFrom,
        LocalDate showDateTo,
        LocalDateTime bookingTimeFrom,
        LocalDateTime bookingTimeTo) {}
