package com.chalchitraghar.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.exception.InvalidSeatSelectionException;
import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.exception.SeatAlreadyBookedException;
import com.chalchitraghar.exception.SeatLockedException;
import com.chalchitraghar.model.Seat;
import com.chalchitraghar.model.enums.SeatStatus;
import com.chalchitraghar.repository.SeatRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for seat booking validation and lock management.
 * Handles concurrent seat locking with pessimistic locking to prevent double-booking.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class BookingService {

    /**
     * Duration in minutes that a seat lock remains valid before expiring.
     */
    private static final int LOCK_DURATION_MINUTES = 10;

    private final SeatRepository seatRepository;

    /**
     * Validates and locks seats for a show.
     * This operation is atomic and transactional.
     * 
     * @param showId The ID of the show
     * @param seatIds List of seat IDs to validate and lock
     * @param userId The ID of the user requesting the lock (for future user-aware locking)
     * @throws ResourceNotFoundException if show or any seat is not found
     * @throws InvalidSeatSelectionException if seats don't belong to the show or validation fails
     * @throws SeatAlreadyBookedException if any seat is already booked
     * @throws SeatLockedException if any seat is currently locked (and not expired)
     */
    public void validateAndLockSeats(Long showId, List<Long> seatIds, Long userId) {
        if (seatIds == null || seatIds.isEmpty()) {
            throw new InvalidSeatSelectionException("At least one seat must be selected");
        }

        // Fetch seats with pessimistic write lock to prevent concurrent modifications
        List<Seat> seats = seatRepository.findByShowIdAndSeatIdsWithLock(showId, seatIds);

        // Validate all seats exist
        if (seats.size() != seatIds.size()) {
            Set<Long> foundSeatIds = seats.stream()
                    .map(Seat::getId)
                    .collect(Collectors.toSet());
            List<Long> missingSeatIds = seatIds.stream()
                    .filter(id -> !foundSeatIds.contains(id))
                    .toList();
            throw new InvalidSeatSelectionException(
                    String.format("Seats not found: %s", missingSeatIds)
            );
        }

        boolean allSeatsBelongToShow = seats.stream()
                .allMatch(seat -> seat.getShow().getId().equals(showId));
        if (!allSeatsBelongToShow) {
            throw new InvalidSeatSelectionException(
                    "All seats must belong to the same show"
            );
        }

        LocalDateTime now = LocalDateTime.now();

        for (Seat seat : seats) {
            if (seat.getSeatStatus() == SeatStatus.LOCKED) {
                if (seat.getLockExpiresAt() != null && seat.getLockExpiresAt().isAfter(now)) {
                    throw new SeatLockedException(
                            String.format("Seat %s (%s) is currently locked by another user", 
                                    seat.getSeatCode(), seat.getId())
                    );
                } else {
                    seat.setSeatStatus(SeatStatus.AVAILABLE);
                    seat.setLockedAt(null);
                    seat.setLockExpiresAt(null);
                }
            }

            if (seat.getSeatStatus() == SeatStatus.BOOKED) {
                throw new SeatAlreadyBookedException(
                        String.format("Seat %s (%s) is already booked", 
                                seat.getSeatCode(), seat.getId())
                );
            }

            // Validate seat is available
            if (seat.getSeatStatus() != SeatStatus.AVAILABLE) {
                throw new InvalidSeatSelectionException(
                        String.format("Seat %s (%s) is not available for booking. Current status: %s", 
                                seat.getSeatCode(), seat.getId(), seat.getSeatStatus())
                );
            }

            seat.setSeatStatus(SeatStatus.LOCKED);
            seat.setLockedAt(now);
            seat.setLockExpiresAt(now.plusMinutes(LOCK_DURATION_MINUTES));
        }

        // Save all seats (locks are persisted)
        seatRepository.saveAll(seats);
    }

    /**
     * Releases locks on seats, setting them back to AVAILABLE status.
     * Used when a booking is cancelled or expires.
     *
     * @param seatIds list of seat IDs to release
     */
    public void releaseSeatLocks(List<Long> seatIds) {
        if (seatIds == null || seatIds.isEmpty()) {
            return;
        }

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
