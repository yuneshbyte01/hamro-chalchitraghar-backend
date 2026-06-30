package com.chalchitraghar.modules.halls.service.impl;

import com.chalchitraghar.modules.halls.service.SeatLayoutService;
import com.chalchitraghar.modules.seats.entity.Seat;
import org.springframework.stereotype.Service;

import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.modules.halls.entity.Hall;
import com.chalchitraghar.modules.halls.entity.SeatTemplate;
import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.modules.halls.repository.HallRepository;
import com.chalchitraghar.modules.halls.repository.SeatTemplateRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatLayoutServiceImpl implements SeatLayoutService {

    private final SeatTemplateRepository seatTemplateRepository;
    private final HallRepository hallRepository;

    @Override
    @Transactional
    public void generateSeatTemplates(Long hallId) {
        if (seatTemplateRepository.existsByHallId(hallId)) {
            throw new IllegalArgumentException("Seat layout already exists for this hall");
        }
        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", hallId));
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
