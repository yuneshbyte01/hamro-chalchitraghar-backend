package com.chalchitraghar.modules.bookings.dto.request;

import com.chalchitraghar.modules.shows.entity.Show;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request DTO for validating seat availability before booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingValidationRequest {

    @NotNull(message = "Show ID is required")
    @Schema(example = "1")
    private Long showId;

    @NotEmpty(message = "At least one seat must be selected")
    @Schema(example = "[10, 11]")
    private List<Long> seatIds;
}
