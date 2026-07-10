package com.chalchitraghar.modules.halls.dto.request;

/** Optional filters and ordering for admin seat-template browsing. */
public record SeatTemplateSearchCriteria(
        String search,
        String seatType,
        String row,
        String sortBy,
        String sortDir) {
}
