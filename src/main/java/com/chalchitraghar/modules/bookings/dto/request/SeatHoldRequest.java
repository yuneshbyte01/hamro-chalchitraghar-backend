package com.chalchitraghar.modules.bookings.dto.request;

import java.util.HashSet;
import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for holding seats before booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldRequest {

    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;

    public boolean hasNoDuplicateSeats() {
        return seatIds != null && !seatIds.isEmpty() && new HashSet<>(seatIds).size() == seatIds.size();
    }
}
