package com.chalchitraghar.modules.shows;

import com.chalchitraghar.modules.movies.MovieResponse;
import com.chalchitraghar.modules.halls.HallResponse;
import com.chalchitraghar.modules.shows.ShowStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing show information with nested movie and hall details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponse {
    
    private Long id;
    private MovieResponse movie;
    private HallResponse hall;
    private ShowStatus status;
    private LocalDate showDate;
    private LocalTime showTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
