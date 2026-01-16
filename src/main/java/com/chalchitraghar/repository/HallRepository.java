package com.chalchitraghar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chalchitraghar.model.Hall;

public interface HallRepository extends JpaRepository<Hall, Long> {

    boolean existsByName(String name);

    List<Hall> findAllByIsActiveTrue();
}
