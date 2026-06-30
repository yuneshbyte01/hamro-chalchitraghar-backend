package com.chalchitraghar.modules.seats.entity;

import com.chalchitraghar.modules.shows.entity.Show;

import com.chalchitraghar.shared.GenericEntity;

import java.time.LocalDateTime;

import com.chalchitraghar.modules.seats.enums.SeatStatus;
import com.chalchitraghar.modules.seats.enums.SeatType;

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

/**
 * Represents a seat within a show, tracking its availability and booking status.
*/
@Entity
@Table(name = "seats")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends GenericEntity {

    /**
     * Show associated with this seat.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    @NotNull(message = "Show is required")
    private Show show;

    /**
     * Seat number within the row.
     */
    @Column(nullable = false)
    @NotNull(message = "Seat number is required")
    private Integer seatNumber;

    /**
     * Row label for this seat.
     */
    @Column(nullable = false)
    @NotNull(message = "Row label is required")
    private String rowLabel;

    /**
     * Unique code for this seat.
     */
    @Column(nullable = false)
    @NotNull(message = "Seat code is required")
    private String seatCode;

    /**
     * Type of seat (PREMIUM, PLATINUM).
     */
    @Column(nullable = false)
    @NotNull(message = "Seat type is required")
    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    /**
     * Price for this seat. Must be greater than zero.
     */
    @Column(nullable = false)
    @NotNull(message = "Price is required")
    @Positive(message = "Price must be greater than 0")
    private Double price;

    /**
     * Current availability status. Automatically set to AVAILABLE on creation.
     */
    @Column(nullable = false)
    @NotNull(message = "Seat status is required")
    @Enumerated(EnumType.STRING)
    private SeatStatus seatStatus;

    /**
     * Position index of the seat in the hall.
     */
    @Column(nullable = false)
    @NotNull(message = "Position index is required")
    private Integer positionIndex;

    /**
     * Timestamp when the seat was locked for reservation. Null if not locked.
     */
    @Column(nullable = true)
    private LocalDateTime lockedAt;

    /**
     * Timestamp when the seat lock expires. Null if not locked.
     */
    @Column(nullable = true)
    private LocalDateTime lockExpiresAt;

    /**
     * Lifecycle callback invoked before entity persistence.
     * Sets default status to AVAILABLE.
     */
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.seatStatus = SeatStatus.AVAILABLE;
    }
}
