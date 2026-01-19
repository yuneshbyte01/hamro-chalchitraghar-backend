package com.chalchitraghar.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing booking validation results.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingValidationResponse {

    private String message;
    private Long showId;
    /**
     * Number of seats successfully locked for booking.
     */
    private Integer lockedSeatCount;
}
