package com.chalchitraghar.modules.seats.service.impl;

import com.chalchitraghar.modules.seats.service.SeatService;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.modules.seats.dto.response.SeatResponse;
import com.chalchitraghar.modules.seats.mapper.SeatMapper;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.time.Clock;
import com.chalchitraghar.modules.shows.service.ShowLifecycleService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final SeatMapper seatMapper;
    private final ShowRepository showRepository;
    private final ShowLifecycleService showLifecycleService;
    private final Clock clock;

    @Override
    @Transactional
    public List<SeatResponse> getAllSeatsForShow(Long showId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        ShowStatus effectiveStatus = showLifecycleService.effectiveStatus(show);
        if (effectiveStatus == ShowStatus.CANCELLED || effectiveStatus == ShowStatus.COMPLETED) {
            throw new ResourceNotFoundException("Show", showId);
        }
        showLifecycleService.assertBookable(show);
        List<Seat> expiredLocks = seatRepository.findExpiredLockedSeatsByShowId(showId, LocalDateTime.now(clock));
        expiredLocks.forEach(seat -> {
            seat.setSeatStatus(SeatStatus.AVAILABLE);
            seat.setLockedAt(null);
            seat.setLockExpiresAt(null);
            seat.setLockedByUserId(null);
        });
        seatRepository.saveAll(expiredLocks);
        return seatRepository.findByShowIdOrderByPositionIndexAsc(showId).stream()
                .map(seatMapper::toResponseDto)
                .toList();
    }
}
