package com.chalchitraghar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import com.chalchitraghar.model.SeatTemplate;

public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {

    boolean existsByHallId(Long hallId);

    List<SeatTemplate> findByHallIdOrderByPositionIndexAsc(Long hallId);

}
