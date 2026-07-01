package com.chalchitraghar.modules.movies.dto.request;

import com.chalchitraghar.modules.halls.enums.Status;
import java.time.LocalDate;

import com.chalchitraghar.modules.movies.enums.MovieStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "Jatra")
    private String title;

    @NotBlank(message = "Genre is required")
    @Schema(example = "Comedy")
    private String genre;

    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration must be at least 1 minute")
    @Schema(example = "125")
    private Integer durationMinutes;

    @NotBlank(message = "Language is required")
    @Schema(example = "Nepali")
    private String language;
    
    @NotBlank(message = "Description is required")
    @Schema(example = "A Nepali comedy movie about an unexpected chain of events.")
    private String description;
    
    @NotBlank(message = "Poster URL is required")
    @Schema(example = "https://example.com/posters/jatra.jpg")
    private String posterUrl;
    
    @NotNull(message = "Release date is required")
    @Schema(example = "2026-08-15")
    private LocalDate releaseDate;
    
    @NotNull(message = "Status is required")
    @Schema(example = "NOW_SHOWING")
    private MovieStatus status;
}
