package com.chalchitraghar.modules.movies.entity;

import com.chalchitraghar.modules.halls.enums.Status;
import com.chalchitraghar.shared.GenericEntity;

import java.time.LocalDate;

import com.chalchitraghar.modules.movies.enums.MovieStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a movie with its metadata, release information, and current status.
 */
@Entity
@Table(name = "movies")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie extends GenericEntity {

    /**
     * Title of the movie. Must be unique and not blank.
     */
    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    private String title;

    /**
     * Genre of the movie. Must be not blank.
     */
    @Column(nullable = false)
    @NotBlank(message = "Genre is required")
    private String genre;

    /**
     * Duration of the movie in minutes. Must be at least 1 minute.
     */
    @Column(nullable = false)
    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    /**
     * Language of the movie. Must be not blank.
     */
    @Column(nullable = false)
    @NotBlank(message = "Language is required")
    private String language;

    /**
     * Description of the movie. Must be not blank.
     */
    @Column(nullable = false)
    @NotBlank(message = "Description is required")
    private String description;

    /**
     * Poster URL of the movie. Must be not blank.
     */
    @Column(nullable = false)
    @NotBlank(message = "Poster URL is required")
    private String posterUrl;

    /**
     * Release date of the movie. Must be not null.
     */
    @Column(nullable = false)
    @NotNull(message = "Release date is required")
    private LocalDate releaseDate;

    /**
     * Status of the movie. Must be not null.
     */
    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private MovieStatus status;
}
