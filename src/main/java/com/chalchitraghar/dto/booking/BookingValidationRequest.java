package com.chalchitraghar.dto.booking;

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
    private Long showId;

    /**
     * List of seat IDs to validate. Must contain at least one seat.
     */
    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;
}
