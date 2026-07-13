package com.chalchitraghar.modules.shows.dto.request;

import java.time.LocalDate;

/** Search and filter criteria for show list endpoints. */
public record ShowSearchCriteria(
        String search, Long movieId, Long hallId, String status, LocalDate showDate) {}
