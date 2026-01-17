package com.chalchitraghar.dto.show;

import com.chalchitraghar.dto.movie.MovieResponseDto;
import com.chalchitraghar.dto.hall.HallResponseDto;
import com.chalchitraghar.model.enums.ShowStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowResponseDto {
    
    private Long id;
    private MovieResponseDto movie;
    private HallResponseDto hall;
    private Double price;
    private ShowStatus status;
    private LocalDate showDate;
    private LocalTime showTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}