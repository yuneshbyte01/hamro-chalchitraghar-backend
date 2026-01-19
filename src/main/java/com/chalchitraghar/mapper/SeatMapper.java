package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.seat.SeatResponse;
import com.chalchitraghar.model.Seat;

@Component
public class SeatMapper {

    public SeatResponse toResponseDto(Seat seat) {
        if (seat == null) {
            return null;
        }

        SeatResponse dto = new SeatResponse();
        dto.setId(seat.getId());
        dto.setRowLabel(seat.getRowLabel());
        dto.setSeatNumber(seat.getSeatNumber());
        dto.setSeatCode(seat.getSeatCode());
        dto.setSeatType(seat.getSeatType());
        dto.setPositionIndex(seat.getPositionIndex());
        dto.setSeatStatus(seat.getSeatStatus());
        return dto;
    }
}
