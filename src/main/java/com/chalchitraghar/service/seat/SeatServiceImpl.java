package com.chalchitraghar.service.seat;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.dto.seat.SeatResponse;
import com.chalchitraghar.mapper.SeatMapper;
import com.chalchitraghar.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SeatResponse> getAllSeatsForShow(Long showId) {
        return seatRepository.findByShowIdOrderByPositionIndexAsc(showId).stream()
                .map(seatMapper::toResponseDto)
                .toList();
    }
}
