package com.chalchitraghar.modules.shows.dto.response;

import com.chalchitraghar.modules.halls.dto.response.PublicHallDetailResponse;
import com.chalchitraghar.modules.movies.dto.response.PublicMovieDetailResponse;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(
        description =
                "Public detail for a visible movie show; cancelled and completed shows are hidden")
public class PublicShowDetailResponse {
    private Long id;
    private PublicMovieDetailResponse movie;
    private PublicHallDetailResponse hall;
    private ShowStatus status;
    private LocalDate showDate;
    private LocalTime showTime;
    private LocalTime endTime;
}
