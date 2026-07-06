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

    /**
     * Checks if a hall with the given name already exists.
     *
     * @param name the hall name to check
     * @return true if a hall with this name exists, false otherwise
     */
    boolean existsByName(String name);

    /**
     * Finds all halls with the specified status.
     *
     * @param status the status to filter by
     * @return list of halls matching the status
     */
    List<Hall> findAllByStatus(Status status);
}
