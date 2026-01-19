package com.chalchitraghar.dto.booking;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingValidationResponseDto {

    private String message;
    private Long showId;
    private Integer lockedSeatCount;
}
