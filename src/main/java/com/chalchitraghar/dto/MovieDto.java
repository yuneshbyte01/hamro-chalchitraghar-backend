package com.chalchitraghar.dto;

import java.time.LocalDate;

import com.chalchitraghar.model.enums.MovieStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieDto {

    @NotBlank(message = "Title is required")
    private String title;
    @NotBlank(message = "Genre is required")
    private String genre;
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
