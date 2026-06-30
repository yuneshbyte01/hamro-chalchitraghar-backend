package com.chalchitraghar.modules.seats;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chalchitraghar.modules.seats.Seat;

import jakarta.persistence.LockModeType;

/**
 * Repository interface for Seat entity persistence operations.
 */
public interface SeatRepository extends JpaRepository<Seat, Long> {

    /**
     * Checks if any seats exist for the given show.
     *
     * @param showId the show ID to check
     * @return true if seats exist for this show, false otherwise
     */
    boolean existsByShowId(Long showId);

    /**
     * Finds all seats for a show, ordered by position index ascending.
     *
     * @param showId the show ID
     * @return list of seats for the show, ordered by position
     */
    List<Seat> findByShowIdOrderByPositionIndexAsc(Long showId);

    /**
     * Finds seats by IDs with pessimistic write lock for concurrent access control.
     *
     * @param seatIds list of seat IDs to retrieve
     * @return list of locked seats
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")
    List<Seat> findByIdsWithLock(@Param("seatIds") List<Long> seatIds);

    /**
     * Finds seats by show ID and seat IDs with pessimistic write lock.
     * Used for atomic seat locking during booking validation.
     *
     * @param showId the show ID
     * @param seatIds list of seat IDs to retrieve
     * @return list of locked seats belonging to the specified show
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.id IN :seatIds")
    List<Seat> findByShowIdAndSeatIdsWithLock(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);

    /**
     * Finds expired locked seats.
     *
     * @param now the current time
     * @return list of expired locked seats
     */
    @Query("SELECT s FROM Seat s WHERE s.seatStatus = 'LOCKED' AND s.lockExpiresAt < :now")
    List<Seat> findExpiredLockedSeats(@Param("now") LocalDateTime now);
}
