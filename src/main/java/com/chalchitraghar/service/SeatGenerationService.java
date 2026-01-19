package com.chalchitraghar.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import jakarta.transaction.Transactional;
import com.chalchitraghar.repository.SeatRepository;
import com.chalchitraghar.model.Show;
import com.chalchitraghar.model.Seat;
import com.chalchitraghar.model.enums.SeatStatus;
import com.chalchitraghar.repository.SeatTemplateRepository;
import com.chalchitraghar.repository.ShowRepository;
import com.chalchitraghar.exception.ResourceNotFoundException;
import java.util.List;
import com.chalchitraghar.model.SeatTemplate;

/**
 * Service for generating seats for shows based on hall seat templates.
 */
@Service
@RequiredArgsConstructor
public class SeatGenerationService {

    private final SeatRepository seatRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final ShowRepository showRepository;

    /**
     * Generates seats for a show based on the hall's seat templates.
     *
     * @param showId the show ID
     * @throws IllegalStateException if seats already exist for this show
     * @throws ResourceNotFoundException if show or seat templates are not found
     */
    @Transactional
    public void generateSeatsForShow(Long showId) {

        if (seatRepository.existsByShowId(showId)) {
            throw new IllegalStateException("Seats already generated for this show");
        }

        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));

        List<SeatTemplate> templates = seatTemplateRepository
                .findByHallIdOrderByPositionIndexAsc(show.getHall().getId());
        
        if (templates.isEmpty()) {
            throw new ResourceNotFoundException("Seat template", show.getHall().getId());
        }

        for (SeatTemplate template : templates) {
            Seat seat = Seat.builder()
                    .show(show)
                    .rowLabel(template.getRowLabel())
                    .seatNumber(template.getSeatNumber())
                    .seatCode(template.getSeatCode())
                    .seatType(template.getSeatType())
                    .positionIndex(template.getPositionIndex())
                    .seatStatus(SeatStatus.AVAILABLE)
                    .build();

            seatRepository.save(seat);
        }
    }
}
