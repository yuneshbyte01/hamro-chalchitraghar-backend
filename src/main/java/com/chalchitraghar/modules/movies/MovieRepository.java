package com.chalchitraghar.modules.movies;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.chalchitraghar.modules.movies.Movie;
import com.chalchitraghar.modules.movies.MovieStatus;

/**
 * Repository interface for Movie entity persistence operations.
 */
public interface MovieRepository extends JpaRepository<Movie, Long> {
    
    /**
     * Finds a movie by title and release date.
     *
     * @param title the movie title
     * @param releaseDate the release date
     * @return optional movie matching the criteria
     */
    Optional<Movie> findByTitleAndReleaseDate(String title, LocalDate releaseDate);
    
    /**
     * Finds all movies with the specified status, ordered by release date ascending.
     *
     * @param status the movie status to filter by
     * @return list of movies matching the status, ordered by release date
     */
    List<Movie> findAllByStatusOrderByReleaseDateAsc(MovieStatus status);
}
