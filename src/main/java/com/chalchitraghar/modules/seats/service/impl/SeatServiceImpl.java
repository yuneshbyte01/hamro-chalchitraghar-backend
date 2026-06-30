package com.chalchitraghar.modules.seats.service.impl;

import com.chalchitraghar.modules.seats.service.SeatService;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import com.chalchitraghar.modules.seats.mapper.SeatMapper;
import com.chalchitraghar.modules.seats.repository.SeatRepository;

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
