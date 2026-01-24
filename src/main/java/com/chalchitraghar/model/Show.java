package com.chalchitraghar.model;

import java.time.LocalDate;
import java.time.LocalTime;

import com.chalchitraghar.model.enums.ShowStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a scheduled movie screening in a specific hall with pricing and timing.
 */
@Entity
@Table(name = "shows")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show extends GenericEntity {

    /**
     * Movie associated with this show.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    @NotNull(message = "Movie is required")
    private Movie movie;

    /**
     * Hall associated with this show.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    @NotNull(message = "Hall is required")
    private Hall hall;

    /**
     * Current status of the show. Automatically set to SCHEDULED on creation.
     */
    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private ShowStatus status;

    /**
     * Show date of the show. Must be not null.
     */
    @Column(nullable = false)
    @NotNull(message = "Show date is required")
    private LocalDate showDate;

    /**
     * Show time of the show. Must be not null.
     */
    @Column(nullable = false)
    @NotNull(message = "Show time is required")
    private LocalTime showTime;

    /**
     * End time of the show. Must be not null.
     */
    @Column(nullable = false)
    @NotNull(message = "End time is required")
    private LocalTime endTime;

    /**
     * Lifecycle callback invoked before entity persistence.
     * Sets default status to SCHEDULED.
     */
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.status = ShowStatus.SCHEDULED;
    }
}