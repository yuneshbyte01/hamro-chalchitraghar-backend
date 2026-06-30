package com.chalchitraghar.modules.shows.service.impl;

import com.chalchitraghar.modules.shows.service.SeatGenerationService;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.halls.entity.SeatTemplate;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.halls.repository.SeatTemplateRepository;
import com.chalchitraghar.modules.shows.repository.ShowRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatGenerationServiceImpl implements SeatGenerationService {

    private static final double PRICE_PLATINUM = 500.0;
    private static final double PRICE_PREMIUM = 750.0;

    private final SeatRepository seatRepository;
    private final SeatTemplateRepository seatTemplateRepository;
    private final ShowRepository showRepository;

    @Override
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
        Map<SeatType, Double> priceMap = Map.of(SeatType.PLATINUM, PRICE_PLATINUM, SeatType.PREMIUM, PRICE_PREMIUM);
        for (SeatTemplate template : templates) {
            Double seatPrice = priceMap.get(template.getSeatType());
            Seat seat = Seat.builder()
                    .show(show)
                    .rowLabel(template.getRowLabel())
                    .seatNumber(template.getSeatNumber())
                    .seatCode(template.getSeatCode())
                    .seatType(template.getSeatType())
                    .price(seatPrice)
                    .positionIndex(template.getPositionIndex())
                    .seatStatus(SeatStatus.AVAILABLE)
                    .build();
            seatRepository.save(seat);
        }
    }
}
