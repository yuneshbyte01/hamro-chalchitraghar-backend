package com.chalchitraghar.modules.bookings.dto.request;

import com.chalchitraghar.modules.shows.entity.Show;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a new booking.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingRequest {

    @NotNull(message = "Show ID is required")
    private Long showId;

    @NotEmpty(message = "At least one seat must be selected")
    private List<Long> seatIds;

    /**
     * Validates that seatIds contains no duplicates.
     * This method should be called before processing the request.
     *
     * @return true if seatIds has no duplicates, false otherwise
     */
    public boolean hasNoDuplicateSeats() {
        if (seatIds == null || seatIds.isEmpty()) {
            return false;
        }
        Set<Long> uniqueSeatIds = new HashSet<>(seatIds);
        return uniqueSeatIds.size() == seatIds.size();
    }
}
