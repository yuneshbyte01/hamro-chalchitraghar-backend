package com.chalchitraghar.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import java.util.List;
import com.chalchitraghar.repository.SeatRepository;
import com.chalchitraghar.mapper.SeatMapper;
import com.chalchitraghar.dto.seat.SeatResponse;

/**
 * Service for seat retrieval operations.
 */
@Service
@RequiredArgsConstructor
public class SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;

    /**
     * Retrieves all seats for a show, ordered by position index.
     *
     * @param showId the show ID
     * @return list of seat responses for the show
     */
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<SeatResponse> getAllSeatsForShow(Long showId) {
        return seatRepository.findByShowIdOrderByPositionIndexAsc(showId)
                .stream()
                .map(seatMapper::toResponseDto)
                .toList();
    }
}
