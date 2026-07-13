package com.chalchitraghar.modules.movies.dto.response;

import com.chalchitraghar.modules.movies.enums.MovieStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Public movie detail response without internal audit metadata. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicMovieDetailResponse {
    private Long id;
    private String title;
    private String genre;
    private Integer durationMinutes;
    private String language;
    private String description;
    private String posterUrl;
    private LocalDate releaseDate;
    private MovieStatus status;
}
