package com.chalchitraghar.service.hall;

import org.springframework.stereotype.Service;

import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.model.Hall;
import com.chalchitraghar.model.SeatTemplate;
import com.chalchitraghar.model.enums.SeatType;
import com.chalchitraghar.repository.HallRepository;
import com.chalchitraghar.repository.SeatTemplateRepository;

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
