package com.chalchitraghar.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.chalchitraghar.repository.SeatRepository;
import com.chalchitraghar.mapper.SeatMapper;
import com.chalchitraghar.dto.seat.SeatResponseDto;
import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.model.Seat;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    // Get all seats for a show
    public List<SeatResponseDto> getAllSeatsForShow(Long showId) {
        return seatRepository.findByShowIdOrderByPositionIndexAsc(showId)
                .stream()
                .map(seatMapper::toResponseDto)
                .toList();
    }
}
