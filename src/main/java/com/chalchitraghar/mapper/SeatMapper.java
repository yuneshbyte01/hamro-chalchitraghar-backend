package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.seat.SeatResponseDto;
import com.chalchitraghar.model.Seat;

@Component
public class SeatMapper {

    public SeatResponseDto toResponseDto(Seat seat) {
        if (seat == null) {
            return null;
        }

        SeatResponseDto dto = new SeatResponseDto();
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
