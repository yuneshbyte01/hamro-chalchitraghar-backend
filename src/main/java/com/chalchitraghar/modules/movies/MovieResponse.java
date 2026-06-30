package com.chalchitraghar.modules.movies;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.chalchitraghar.modules.movies.MovieStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing movie information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponse {
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
