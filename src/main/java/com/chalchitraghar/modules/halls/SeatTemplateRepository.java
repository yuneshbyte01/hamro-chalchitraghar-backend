package com.chalchitraghar.modules.halls;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.chalchitraghar.modules.halls.SeatTemplate;

/**
 * Repository interface for SeatTemplate entity persistence operations.
 */
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

}
