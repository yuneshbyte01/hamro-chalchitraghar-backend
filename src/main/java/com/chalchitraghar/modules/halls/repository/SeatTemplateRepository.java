package com.chalchitraghar.modules.halls.repository;

import com.chalchitraghar.modules.halls.entity.SeatTemplate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository interface for SeatTemplate entity persistence operations. */
public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {

    /**
     * Checks if seat templates exist for the given hall.
     *
     * @param hallId the hall ID to check
     * @return true if templates exist for this hall, false otherwise
     */
    boolean existsByHallId(Long hallId);

    /**
     * Finds all seat templates for a hall, ordered by position index ascending.
     *
     * @param hallId the hall ID
     * @return list of seat templates for the hall, ordered by position
     */
    List<SeatTemplate> findByHallIdOrderByPositionIndexAsc(Long hallId);

    long countByHallId(Long hallId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM SeatTemplate template WHERE template.hall.id = :hallId")
    void deleteByHallId(@Param("hallId") Long hallId);
}
