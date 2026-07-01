package com.chalchitraghar.modules.bookings.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for a successful seat hold.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatHoldResponse {

    private String message;
    private Long showId;
    private Integer heldSeatCount;
    private LocalDateTime holdExpiresAt;
}
