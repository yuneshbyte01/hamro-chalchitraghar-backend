package com.chalchitraghar.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.chalchitraghar.repository.SeatRepository;
import com.chalchitraghar.mapper.SeatMapper;
import com.chalchitraghar.dto.seat.SeatResponse;

@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<SeatResponse> getAllSeatsForShow(Long showId) {
        return seatRepository.findByShowIdOrderByPositionIndexAsc(showId)
                .stream()
                .map(seatMapper::toResponseDto)
                .toList();
    }
}
