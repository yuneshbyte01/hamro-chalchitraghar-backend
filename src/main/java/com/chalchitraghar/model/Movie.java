package com.chalchitraghar.model;

import java.time.LocalDate;

import com.chalchitraghar.model.enums.MovieStatus;

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

@Entity
@Table(name = "movies")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie extends GenericEntity {

    @Column(nullable = false)
    @NotBlank(message = "Title is required")
    private String title;

    @Column(nullable = false)
    @NotBlank(message = "Genre is required")
    private String genre;

    @Column(nullable = false)
    @NotNull(message = "Duration is required")
    @PositiveOrZero(message = "Duration must be at least 1 minute")
    private Integer durationMinutes;

    @Column(nullable = false)
    @NotBlank(message = "Language is required")
    private String language;

    @Column(nullable = false)
    @NotBlank(message = "Description is required")
    private String description;

    @Column(nullable = false)
    @NotBlank(message = "Poster URL is required")
    private String posterUrl;

    @Column(nullable = false)
    @NotNull(message = "Release date is required")
    private LocalDate releaseDate;

    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private MovieStatus status;
}
