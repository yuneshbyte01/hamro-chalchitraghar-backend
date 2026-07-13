package com.chalchitraghar.modules.shows.dto.response;

import com.chalchitraghar.modules.halls.dto.response.AdminHallDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.AdminMovieDetailResponse;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Administrative show detail, available for every show status")
public class AdminShowDetailResponse {
    private Long id;
    private AdminMovieDetailResponse movie;
    private AdminHallDetailResponse hall;
    private ShowStatus status;
    private LocalDate showDate;
    private LocalTime showTime;
    private LocalTime endTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
