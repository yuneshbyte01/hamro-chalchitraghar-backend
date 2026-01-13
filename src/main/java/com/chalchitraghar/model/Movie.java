package com.chalchitraghar.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.chalchitraghar.model.enums.MovieStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "movies")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    @NotNull(message = "Created at is required")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @NotNull(message = "Updated at is required")
    private LocalDateTime updatedAt;

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
