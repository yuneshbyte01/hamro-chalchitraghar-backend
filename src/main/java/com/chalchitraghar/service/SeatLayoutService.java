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

/**
 * Service for generating seat layout templates for halls.
 * Creates a standard layout: Row A (8 PREMIUM seats), Rows B-J (20 PLATINUM seats each).
 */
@Service
@RequiredArgsConstructor
public class SeatLayoutService {

    private final SeatTemplateRepository seatTemplateRepository;
    private final HallRepository hallRepository;

    /**
     * Generates seat templates for a hall with a standard layout configuration.
     *
     * @param hallId the hall ID
     * @throws IllegalArgumentException if seat layout already exists for this hall
     * @throws ResourceNotFoundException if hall is not found
     */
    @Transactional
    public void generateSeatTemplates(Long hallId) {

        if (seatTemplateRepository.existsByHallId(hallId)) {
            throw new IllegalArgumentException("Seat layout already exists for this hall");
        }

        Hall hall = hallRepository.findById(hallId)
                .orElseThrow(() -> new ResourceNotFoundException("Hall", hallId));

        int index = 0;

        for (int seat = 1; seat <= 8; seat++) {
            seatTemplateRepository.save(buildSeat(
                    hall, "A", seat, SeatType.PREMIUM, index++
            ));
        }

        for (char row = 'B'; row <= 'J'; row++) {
            for (int seat = 1; seat <= 20; seat++) {
                seatTemplateRepository.save(buildSeat(
                        hall, String.valueOf(row), seat,
                        SeatType.PLATINUM, index++
                ));
            }
        }
    }

    /**
     * Builds a seat template with the specified properties.
     *
     * @param hall the hall
     * @param rowLabel the row label
     * @param seatNumber the seat number
     * @param seatType the seat type
     * @param positionIndex the position index
     * @return the built seat template
     */
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
