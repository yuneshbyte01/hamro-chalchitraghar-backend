package com.chalchitraghar.modules.movies.dto.request;

import java.time.LocalDate;

import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.modules.movies.validation.ValidPosterUrl;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 600, message = "Duration must not exceed 600 minutes")
    @Schema(example = "125")
    private Integer durationMinutes;

    @NotBlank(message = "Language is required")
    @Schema(example = "Nepali")
    private String language;
    
    @NotBlank(message = "Description is required")
    @Schema(example = "A Nepali comedy movie about an unexpected chain of events.")
    private String description;
    
    @NotBlank(message = "Poster URL is required")
    @Size(max = 500, message = "Poster URL must not exceed 500 characters")
    @ValidPosterUrl
    @Schema(example = "https://example.com/posters/jatra.jpg")
    private String posterUrl;
    
    @NotNull(message = "Release date is required")
    @Schema(example = "2026-08-15")
    private LocalDate releaseDate;
    
    @NotNull(message = "Status is required")
    @Schema(example = "NOW_SHOWING")
    private MovieStatus status;
}
