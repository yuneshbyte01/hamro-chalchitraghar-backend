package com.chalchitraghar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import com.chalchitraghar.model.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    boolean existsByShowId(Long showId);

    List<Seat> findByShowIdOrderByPositionIndexAsc(Long showId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.id IN :seatIds")
    List<Seat> findByIdsWithLock(@Param("seatIds") List<Long> seatIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Seat s WHERE s.show.id = :showId AND s.id IN :seatIds")
    List<Seat> findByShowIdAndSeatIdsWithLock(@Param("showId") Long showId, @Param("seatIds") List<Long> seatIds);

}
