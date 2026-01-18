package com.chalchitraghar.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.chalchitraghar.model.SeatTemplate;

public interface SeatTemplateRepository extends JpaRepository<SeatTemplate, Long> {

    boolean existsByHallId(Long hallId);

}
