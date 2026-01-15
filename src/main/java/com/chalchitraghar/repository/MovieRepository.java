package com.chalchitraghar.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chalchitraghar.model.Movie;
import com.chalchitraghar.model.enums.MovieStatus;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTitleAndReleaseDate(String title, LocalDate releaseDate);
    List<Movie> findAllByStatusOrderByReleaseDateAsc(MovieStatus status);
}
