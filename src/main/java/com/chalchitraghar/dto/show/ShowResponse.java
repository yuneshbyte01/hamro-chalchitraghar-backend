package com.chalchitraghar.dto.show;

import com.chalchitraghar.dto.movie.MovieResponse;
import com.chalchitraghar.dto.hall.HallResponse;
import com.chalchitraghar.model.enums.ShowStatus;

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
    private Double price;
    private ShowStatus status;
    private LocalDate showDate;
    private LocalTime showTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
