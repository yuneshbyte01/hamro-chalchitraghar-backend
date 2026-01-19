package com.chalchitraghar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.chalchitraghar.model.enums.ShowStatus;
import com.chalchitraghar.model.Show;

/**
 * Repository interface for Show entity persistence operations.
 */
public interface ShowRepository extends JpaRepository<Show, Long> {

    /**
     * Checks if there's an overlapping show for the same hall on the same date.
     * A show overlaps if: new show starts before existing show ends AND new show ends after existing show starts.
     *
     * @param hallId the hall ID to check
     * @param showDate the show date
     * @param showTime the start time of the show
     * @param endTime the end time of the show
     * @param excludeShowId optional show ID to exclude from the check (for updates)
     * @return true if an overlapping show exists, false otherwise
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

    /**
     * Finds all shows for a specific hall.
     *
     * @param hallId the hall ID
     * @return list of shows for the hall
     */
    List<Show> findByHallId(Long hallId);

    /**
     * Finds all shows for a specific movie.
     *
     * @param movieId the movie ID
     * @return list of shows for the movie
     */
    List<Show> findByMovieId(Long movieId);

    /**
     * Finds all shows for a specific movie on a specific date.
     *
     * @param movieId the movie ID
     * @param showDate the show date
     * @return list of shows matching the criteria
     */
    List<Show> findByMovieIdAndShowDate(Long movieId, LocalDate showDate);

    /**
     * Finds shows by movie, date, time, and status list.
     *
     * @param movieId the movie ID
     * @param showDate the show date
     * @param showTime the show time
     * @param statuses list of statuses to filter by
     * @return list of shows matching all criteria
     */
    List<Show> findByMovieIdAndShowDateAndShowTimeAndStatusIn(
            Long movieId,
            LocalDate showDate,
            LocalTime showTime,
            List<ShowStatus> statuses
    );
}

