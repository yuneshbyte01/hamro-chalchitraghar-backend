package com.chalchitraghar.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

import com.chalchitraghar.repository.SeatTemplateRepository;
import com.chalchitraghar.repository.HallRepository;
import jakarta.transaction.Transactional;
import com.chalchitraghar.model.Hall;
import com.chalchitraghar.model.SeatTemplate;
import com.chalchitraghar.model.enums.SeatType;
import com.chalchitraghar.exception.ResourceNotFoundException;

@Service
@RequiredArgsConstructor
public class SeatLayoutService {

    private final SeatTemplateRepository seatTemplateRepository;
    private final HallRepository hallRepository;

    @Transactional
    public void generateSeatTemplates(Long hallId) {

        if (seatTemplateRepository.existsByHallId(hallId)) {
            throw new IllegalArgumentException("Seat layout already exists for this hall");
        }

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", hallId));

        int index = 0;

        // PREMIUM – Row A (8 seats)
        for (int seat = 1; seat <= 8; seat++) {
            seatTemplateRepository.save(buildSeat(
                    hall, "A", seat, SeatType.PREMIUM, index++
            ));
        }

        // PLATINUM – Rows B to J (20 seats each)
        for (char row = 'B'; row <= 'J'; row++) {
            for (int seat = 1; seat <= 20; seat++) {
                seatTemplateRepository.save(buildSeat(
                        hall, String.valueOf(row), seat,
                        SeatType.PLATINUM, index++
                ));
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
