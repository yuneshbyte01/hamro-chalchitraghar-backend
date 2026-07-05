package com.chalchitraghar.modules.movies.dto.response;

import java.time.LocalDate;

import com.chalchitraghar.modules.movies.enums.MovieStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public movie list response without internal audit metadata.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicMovieSummaryResponse {
    private Long id;
    private String title;
    private String genre;
    private Integer durationMinutes;
    private String language;
    private String posterUrl;
    private LocalDate releaseDate;
    private MovieStatus status;
}
