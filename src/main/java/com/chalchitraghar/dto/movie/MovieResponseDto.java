package com.chalchitraghar.dto.movie;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.chalchitraghar.model.enums.MovieStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MovieResponseDto {
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