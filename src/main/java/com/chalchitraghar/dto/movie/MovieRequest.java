package com.chalchitraghar.dto.movie;

import java.time.LocalDate;

import com.chalchitraghar.model.enums.MovieStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating or updating a movie.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieRequest {

    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Genre is required")
    private String genre;
    /**
     * Duration in minutes. Must be at least 1 minute.
     */
    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration must be at least 1 minute")
    private Integer durationMinutes;
    @NotBlank(message = "Language is required")
    private String language;
    @NotBlank(message = "Description is required")
    private String description;
    @NotBlank(message = "Poster URL is required")
    private String posterUrl;
    @NotNull(message = "Release date is required")
    private LocalDate releaseDate;
    @NotNull(message = "Status is required")
    private MovieStatus status;
}
