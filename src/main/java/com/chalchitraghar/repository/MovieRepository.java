package com.chalchitraghar.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chalchitraghar.model.Movie;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTitleAndReleaseDate(String title, LocalDate releaseDate);
}
