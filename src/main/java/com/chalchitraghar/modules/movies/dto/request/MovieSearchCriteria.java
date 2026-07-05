package com.chalchitraghar.modules.movies.dto.request;

import java.time.LocalDate;

/**
 * Search and filter criteria for movie list endpoints.
 */
public record MovieSearchCriteria(
        String search,
        String status,
        String genre,
        String language,
        LocalDate releaseDateFrom,
        LocalDate releaseDateTo
) {
}
