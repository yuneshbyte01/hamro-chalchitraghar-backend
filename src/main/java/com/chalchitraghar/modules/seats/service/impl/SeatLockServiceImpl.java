package com.chalchitraghar.modules.seats.service.impl;

import com.chalchitraghar.modules.seats.service.SeatLockService;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.shared.exception.InvalidSeatSelectionException;
import com.chalchitraghar.shared.exception.SeatAlreadyBookedException;
import com.chalchitraghar.shared.exception.SeatLockedException;
import com.chalchitraghar.modules.bookings.dto.response.SeatHoldResponse;
import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.repository.SeatRepository;
import com.chalchitraghar.modules.shows.repository.ShowRepository;
import com.chalchitraghar.modules.shows.entity.Show;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.exception.ShowConflictException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class SeatLockServiceImpl implements SeatLockService {

    private static final int LOCK_DURATION_MINUTES = 10;
    private final SeatRepository seatRepository;
    private final ShowRepository showRepository;

    @Override
    public SeatHoldResponse holdSeats(Long showId, List<Long> seatIds, Long userId) {
        Show show = showRepository.findById(showId)
                .orElseThrow(() -> new ResourceNotFoundException("Show", showId));
        if (show.getStatus() == ShowStatus.CANCELLED || show.getStatus() == ShowStatus.COMPLETED) {
            throw new ShowConflictException("Seats cannot be held for a cancelled or completed show");
        }
        if (seatIds == null || seatIds.isEmpty()) {
            throw new InvalidSeatSelectionException("At least one seat must be selected");
        }
        if (new HashSet<>(seatIds).size() != seatIds.size()) {
            throw new InvalidSeatSelectionException("Seat IDs contain duplicates");
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
        LocalDateTime holdExpiresAt = now.plusMinutes(LOCK_DURATION_MINUTES);
        for (Seat seat : seats) {
            if (seat.getSeatStatus() == SeatStatus.LOCKED) {
                if (isLockExpired(seat, now)) {
                    clearLock(seat);
                } else if (userId.equals(seat.getLockedByUserId())) {
                    seat.setLockedAt(now);
                    seat.setLockExpiresAt(holdExpiresAt);
                    continue;
                } else {
                    throw new SeatLockedException(
                            String.format("Seat %s (%s) is currently locked by another user", seat.getSeatCode(), seat.getId()));
                }
            }
            if (seat.getSeatStatus() == SeatStatus.BOOKED) {
                throw new SeatAlreadyBookedException(
                        String.format("Seat %s (%s) is already booked", seat.getSeatCode(), seat.getId()));
            }
            if (seat.getSeatStatus() == SeatStatus.RESERVED) {
                throw new SeatAlreadyBookedException(
                        String.format("Seat %s (%s) is already reserved", seat.getSeatCode(), seat.getId()));
            }
            if (seat.getSeatStatus() != SeatStatus.AVAILABLE) {
                throw new InvalidSeatSelectionException(
                        String.format("Seat %s (%s) is not available for booking. Current status: %s",
                                seat.getSeatCode(), seat.getId(), seat.getSeatStatus()));
            }
            seat.setSeatStatus(SeatStatus.LOCKED);
            seat.setLockedAt(now);
            seat.setLockExpiresAt(holdExpiresAt);
            seat.setLockedByUserId(userId);
        }
        seatRepository.saveAll(seats);
        return new SeatHoldResponse("Seats held successfully", showId, seats.size(), holdExpiresAt);
    }

    @Override
    public void releaseSeatLocks(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) return;
        List<Seat> seats = seatRepository.findByIdsWithLock(seatIds);
        for (Seat seat : seats) {
            if (seat.getSeatStatus() == SeatStatus.LOCKED) {
                clearLock(seat);
            }
        }
        seatRepository.saveAll(seats);
    }

    @Override
    public int releaseExpiredSeatLocks() {
        List<Seat> seats = seatRepository.findExpiredLockedSeats(LocalDateTime.now());
        for (Seat seat : seats) {
            clearLock(seat);
        }
        seatRepository.saveAll(seats);
        return seats.size();
    }

    private boolean isLockExpired(Seat seat, LocalDateTime now) {
        return seat.getLockExpiresAt() == null || !seat.getLockExpiresAt().isAfter(now);
    }

    private void clearLock(Seat seat) {
        seat.setSeatStatus(SeatStatus.AVAILABLE);
        seat.setLockedAt(null);
        seat.setLockExpiresAt(null);
        seat.setLockedByUserId(null);
    }
}
