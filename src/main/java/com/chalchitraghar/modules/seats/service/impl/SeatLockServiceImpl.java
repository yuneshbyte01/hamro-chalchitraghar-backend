package com.chalchitraghar.modules.seats.service.impl;

import com.chalchitraghar.modules.seats.service.SeatLockService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.shared.exception.InvalidSeatSelectionException;
import com.chalchitraghar.shared.exception.SeatAlreadyBookedException;
import com.chalchitraghar.shared.exception.SeatLockedException;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatLockServiceImpl implements SeatLockService {

    private static final int LOCK_DURATION_MINUTES = 10;
    private final SeatRepository seatRepository;

    @Override
    public void validateAndLockSeats(Long showId, List<Long> seatIds, Long userId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new InvalidSeatSelectionException("At least one seat must be selected");
        }
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(showId, seatIds);
        if (seats.size() != seatIds.size()) {
            Set<Long> foundSeatIds = seats.stream().map(Seat::getId).collect(Collectors.toSet());
            List<Long> missingSeatIds = seatIds.stream().filter(id -> !foundSeatIds.contains(id)).toList();
            throw new InvalidSeatSelectionException(String.format("Seats not found: %s", missingSeatIds));
        }
        if (!seats.stream().allMatch(seat -> seat.getShow().getId().equals(showId))) {
            throw new InvalidSeatSelectionException("All seats must belong to the same show");
        }
        LocalDateTime now = LocalDateTime.now();
        for (Seat seat : seats) {
            if (seat.getSeatStatus() == SeatStatus.LOCKED) {
                if (seat.getLockExpiresAt() != null && seat.getLockExpiresAt().isAfter(now)) {
                    throw new SeatLockedException(
                            String.format("Seat %s (%s) is currently locked by another user", seat.getSeatCode(), seat.getId()));
                } else {
                    seat.setSeatStatus(SeatStatus.AVAILABLE);
                    seat.setLockedAt(null);
                    seat.setLockExpiresAt(null);
                }
            }
            if (seat.getSeatStatus() == SeatStatus.BOOKED) {
                throw new SeatAlreadyBookedException(
                        String.format("Seat %s (%s) is already booked", seat.getSeatCode(), seat.getId()));
            }
            if (seat.getSeatStatus() != SeatStatus.AVAILABLE) {
                throw new InvalidSeatSelectionException(
                        String.format("Seat %s (%s) is not available for booking. Current status: %s",
                                seat.getSeatCode(), seat.getId(), seat.getSeatStatus()));
            }
            seat.setSeatStatus(SeatStatus.LOCKED);
            seat.setLockedAt(now);
            seat.setLockExpiresAt(now.plusMinutes(LOCK_DURATION_MINUTES));
        }
        seatRepository.saveAll(seats);
    }

    @Override
    public void releaseSeatLocks(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return;
        List<Seat> seats = seatRepository.findByIdsWithLock(seatIds);
        LocalDateTime now = LocalDateTime.now();
        for (Seat seat : seats) {
            if (seat.getSeatStatus() == SeatStatus.LOCKED) {
                if (seat.getLockExpiresAt() == null || seat.getLockExpiresAt().isAfter(now)) {
                    seat.setSeatStatus(SeatStatus.AVAILABLE);
                    seat.setLockedAt(null);
                    seat.setLockExpiresAt(null);
                }
            }
        }
        seatRepository.saveAll(seats);
    }
}
