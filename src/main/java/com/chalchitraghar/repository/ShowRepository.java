package com.chalchitraghar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.chalchitraghar.model.enums.ShowStatus;
import com.chalchitraghar.model.Show;

public interface ShowRepository extends JpaRepository<Show, Long> {

    /**
     * Checks if there's an overlapping show for the same hall on the same date.
     * A show overlaps if: new show starts before existing show ends AND new show ends after existing show starts
     */
    @Query("""
        SELECT COUNT(s) > 0
        FROM Show s
        WHERE s.hall.id = :hallId
          AND s.showDate = :showDate
          AND s.status <> 'CANCELLED'
          AND (:showTime < s.endTime AND :endTime > s.showTime)
          AND (:excludeShowId IS NULL OR s.id != :excludeShowId)
    """)
    boolean existsOverlappingShow(
            @Param("hallId") Long hallId,
            @Param("showDate") LocalDate showDate,
            @Param("showTime") LocalTime showTime,
            @Param("endTime") LocalTime endTime,
            @Param("excludeShowId") Long excludeShowId
    );

    List<Show> findByHallId(Long hallId);

    List<Show> findByMovieId(Long movieId);

    List<Show> findByMovieIdAndShowDateAndShowTimeAndStatusIn(
            Long movieId,
            LocalDate showDate,
            LocalTime showTime,
            List<ShowStatus> statuses
    );
}

