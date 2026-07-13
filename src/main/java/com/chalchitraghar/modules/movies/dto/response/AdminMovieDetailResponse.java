package com.chalchitraghar.modules.movies.dto.response;

import com.chalchitraghar.modules.movies.enums.MovieStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin movie detail response including audit metadata. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminMovieDetailResponse {
    private Long id;
    private String title;
    private String genre;
    private Integer durationMinutes;
    private String language;
    private String description;
    private String posterUrl;
    private LocalDate releaseDate;
    private MovieStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
