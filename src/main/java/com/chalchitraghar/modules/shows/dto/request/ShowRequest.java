package com.chalchitraghar.modules.shows.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for creating or updating a show. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShowRequest {

    @NotNull(message = "Movie ID is required")
    @Schema(example = "1")
    private Long movieId;

    @NotNull(message = "Hall ID is required")
    @Schema(example = "1")
    private Long hallId;

    @NotNull(message = "Show date is required")
    @Schema(example = "2026-08-20")
    private LocalDate showDate;

    @NotNull(message = "Show time is required")
    @Schema(example = "18:30:00")
    private LocalTime showTime;

    @NotNull(message = "End time is required")
    @Schema(example = "21:00:00")
    private LocalTime endTime;
}
