package com.chalchitraghar.modules.movies.entity;

import com.chalchitraghar.modules.movies.enums.MovieStatus;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Represents a movie with its metadata, release information, and current status. */
@Entity
@Table(
        name = "movies",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_movies_title_normalized_release_date",
                        columnNames = {"title_normalized", "release_date"}))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie extends GenericEntity {

    /** Title of the movie. Must be unique and not blank. */
    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    private String title;

    /** Normalized title used for case-insensitive duplicate protection. */
    @Column(name = "title_normalized", nullable = false)
    private String titleNormalized;

    /** Genre of the movie. Must be not blank. */
    @Column(nullable = false)
    @NotBlank(message = "Genre is required")
    private String genre;

    /** Duration of the movie in minutes. Must be at least 1 minute. */
    @Column(nullable = false)
    @NotNull(message = "Duration is required")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 600, message = "Duration must not exceed 600 minutes")
    private Integer durationMinutes;

    /** Language of the movie. Must be not blank. */
    @Column(nullable = false)
    @NotBlank(message = "Language is required")
    private String language;

    /** Description of the movie. Must be not blank. */
    @Column(nullable = false)
    @NotBlank(message = "Description is required")
    private String description;

    /** Poster URL of the movie. Must be not blank. */
    @Column(nullable = false, length = 500)
    @NotBlank(message = "Poster URL is required")
    @Size(max = 500, message = "Poster URL must not exceed 500 characters")
    private String posterUrl;

    /** Release date of the movie. Must be not null. */
    @Column(nullable = false)
    @NotNull(message = "Release date is required")
    private LocalDate releaseDate;

    /** Status of the movie. Must be not null. */
    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private MovieStatus status;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        normalizeTitleForUniqueness();
    }

    @PreUpdate
    private void onMovieUpdate() {
        normalizeTitleForUniqueness();
    }

    private void normalizeTitleForUniqueness() {
        this.titleNormalized = normalizeTitle(title);
    }

    public static String normalizeTitle(String title) {
        if (title == null) {
            return null;
        }
        return title.trim().toLowerCase(Locale.ROOT);
    }
}
