package com.chalchitraghar.modules.movies.dto.response;

import java.time.LocalDate;

import com.chalchitraghar.modules.movies.enums.MovieStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin movie list response with operational fields kept compact.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMovieSummaryResponse {
    private Long id;
    private String title;
    private String genre;
    private String language;
    private LocalDate releaseDate;
    private MovieStatus status;
}
