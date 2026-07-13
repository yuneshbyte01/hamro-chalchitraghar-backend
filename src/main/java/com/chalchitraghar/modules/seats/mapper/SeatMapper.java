package com.chalchitraghar.modules.seats.mapper;

import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import com.chalchitraghar.modules.seats.entity.Seat;
import org.springframework.stereotype.Component;

/** Mapper for converting Seat entity to DTOs. */
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
        dto.setPrice(seat.getPrice());
        dto.setPositionIndex(seat.getPositionIndex());
        dto.setSeatStatus(seat.getSeatStatus());
        return dto;
    }
}
