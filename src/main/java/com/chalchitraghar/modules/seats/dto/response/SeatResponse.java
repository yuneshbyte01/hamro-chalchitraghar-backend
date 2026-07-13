package com.chalchitraghar.modules.seats.dto.response;

import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.enums.SeatType;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO containing seat information and availability status. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SeatResponse {

    private Long id;
    private String rowLabel;
    private Integer seatNumber;
    private String seatCode;
    private SeatType seatType;
    private BigDecimal price;
    private Integer positionIndex;
    private SeatStatus seatStatus;
}
