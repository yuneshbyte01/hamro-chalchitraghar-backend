package com.chalchitraghar.modules.halls.service.impl;

import org.springframework.stereotype.Service;

import java.util.List;

import com.chalchitraghar.modules.halls.dto.response.AdminSeatLayoutResponse;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.entity.SeatTemplate;
import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.modules.halls.repository.HallRepository;
import com.chalchitraghar.modules.halls.repository.SeatTemplateRepository;
import com.chalchitraghar.modules.halls.mapper.SeatTemplateMapper;
import com.chalchitraghar.modules.halls.service.SeatLayoutService;
import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.shared.exception.HallConflictException;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatLayoutServiceImpl implements SeatLayoutService {

    private static final int FIXED_LAYOUT_CAPACITY = 188;

    private final SeatTemplateRepository seatTemplateRepository;
    private final HallRepository hallRepository;
    private final SeatTemplateMapper seatTemplateMapper;

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public AdminSeatLayoutResponse getSeatLayout(Long hallId) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", hallId));
        List<SeatTemplate> templates = seatTemplateRepository.findByHallIdOrderByPositionIndexAsc(hallId);
        if (templates.isEmpty()) {
            throw new ResourceNotFoundException("Seat layout", hallId);
        }
        return seatTemplateMapper.toLayoutResponse(hall, templates);
    }

    @Override
    @Transactional
    public void generateSeatTemplates(Long hallId) {
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", hallId));
        if (hall.getStatus() != Status.ACTIVE) {
            throw new HallConflictException("Cannot generate seat layout for an inactive hall");
        }
        if (seatTemplateRepository.existsByHallId(hallId)) {
            throw new HallConflictException("Seat layout already exists for this hall");
        }
        if (!Integer.valueOf(FIXED_LAYOUT_CAPACITY).equals(hall.getCapacity())) {
            throw new HallConflictException("Seat layout generation currently requires hall capacity to be 188");
        }
        int index = 0;
        for (int seat = 1; seat <= 8; seat++) {
            seatTemplateRepository.save(buildSeat(hall, "A", seat, SeatType.PREMIUM, index++));
        }
        for (char row = 'B'; row <= 'J'; row++) {
            for (int seat = 1; seat <= 20; seat++) {
                seatTemplateRepository.save(buildSeat(hall, String.valueOf(row), seat, SeatType.PLATINUM, index++));
            }
        }
    }

    private SeatTemplate buildSeat(Hall hall, String rowLabel, int seatNumber, SeatType seatType, int positionIndex) {
        return SeatTemplate.builder()
                .hall(hall)
                .rowLabel(rowLabel)
                .seatNumber(seatNumber)
                .seatType(seatType)
                .seatCode(rowLabel + seatNumber)
                .positionIndex(positionIndex)
                .build();
    }
}
