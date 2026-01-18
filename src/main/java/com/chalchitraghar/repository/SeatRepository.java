package com.chalchitraghar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.chalchitraghar.model.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    boolean existsByShowId(Long showId);

    List<Seat> findByShowIdOrderByPositionIndexAsc(Long showId);

}
