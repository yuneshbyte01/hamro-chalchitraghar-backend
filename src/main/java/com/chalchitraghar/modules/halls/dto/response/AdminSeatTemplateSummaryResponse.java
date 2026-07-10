package com.chalchitraghar.modules.halls.dto.response;

import com.chalchitraghar.modules.seats.enums.SeatType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin-facing summary of one seat template in a hall layout. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSeatTemplateSummaryResponse {

    private Long id;
    private String rowLabel;
    private Integer seatNumber;
    private String seatCode;
    private SeatType seatType;
    private Integer positionIndex;
}
