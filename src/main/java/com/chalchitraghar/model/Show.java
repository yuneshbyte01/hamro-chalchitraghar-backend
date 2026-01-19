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
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shows")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Show extends GenericEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "movie_id", nullable = false)
    @NotNull(message = "Movie is required")
    private Movie movie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    @NotNull(message = "Hall is required")
    private Hall hall;

    @Column(nullable = false)
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    @Column(nullable = false)
    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    private ShowStatus status;

    @Column(nullable = false)
    @NotNull(message = "Show date is required")
    private LocalDate showDate;

    @Column(nullable = false)
    @NotNull(message = "Show time is required")
    private LocalTime showTime;

    @Column(nullable = false)
    @NotNull(message = "End time is required")
    private LocalTime endTime;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.status = ShowStatus.SCHEDULED;
    }
}