package com.chalchitraghar.modules.halls.repository;

import java.util.List;

import com.chalchitraghar.modules.halls.enums.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.chalchitraghar.modules.halls.entity.Hall;

/**
 * Repository interface for Hall entity persistence operations.
 */
public interface HallRepository extends JpaRepository<Hall, Long>, JpaSpecificationExecutor<Hall> {

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    /**
     * Finds all halls with the specified status.
     *
     * @param status the status to filter by
     * @return list of halls matching the status
     */
    List<Hall> findAllByStatus(Status status);
}
