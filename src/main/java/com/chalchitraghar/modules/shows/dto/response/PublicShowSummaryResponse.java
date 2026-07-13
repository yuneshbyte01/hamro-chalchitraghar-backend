package com.chalchitraghar.modules.shows.dto.response;

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
@Schema(description = "Public summary of a visible movie show")
public class PublicShowSummaryResponse {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Long hallId;
    private String hallName;
    private ShowStatus status;
    private LocalDate showDate;
    private LocalTime showTime;
    private LocalTime endTime;
}
