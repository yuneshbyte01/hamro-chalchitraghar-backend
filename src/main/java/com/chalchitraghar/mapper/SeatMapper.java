package com.chalchitraghar.mapper;

import org.springframework.stereotype.Component;

import com.chalchitraghar.dto.seat.SeatResponse;
import com.chalchitraghar.model.Seat;

/**
 * Mapper for converting Seat entity to DTOs.
 */
@Component
public class SeatMapper {

    /**
     * Converts a Seat entity to a SeatResponse DTO.
     *
     * @param seat the entity to convert
     * @return the response DTO, or null if seat is null
     */
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
