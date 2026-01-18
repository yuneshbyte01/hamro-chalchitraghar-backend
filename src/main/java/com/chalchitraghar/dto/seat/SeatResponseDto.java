package com.chalchitraghar.dto.seat;

import com.chalchitraghar.model.enums.SeatType;
import com.chalchitraghar.model.enums.SeatStatus;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponseDto {

    private Long id;
    private String rowLabel;
    private Integer seatNumber;
    private String seatCode;
    private SeatType seatType;
    private Integer positionIndex;
    private SeatStatus seatStatus;
}
