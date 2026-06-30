package com.chalchitraghar.modules.halls.entity;

import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.shared.GenericEntity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import com.chalchitraghar.modules.seats.enums.SeatType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

/**
 * Template defining seat configuration for a hall, used to generate actual seats for shows.
 */
@Entity
@Table(name = "seat_templates")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatTemplate extends GenericEntity {

    /**
     * Hall associated with this seat template.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    @NotNull(message = "Hall is required")
    private Hall hall;

    /**
     * Row label for this seat template.
     */
    @Column(nullable = false)
    @NotBlank(message = "Row label is required")
    private String rowLabel;

    /**
     * Seat number for this seat template.
     */
    @Column(nullable = false)
    @NotNull(message = "Seat number is required")
    @Positive(message = "Seat number must be greater than 0")
    private Integer seatNumber;

    /**
     * Generated seat code combining row label and seat number. Automatically maintained.
     */
    @Column(nullable = false)
    @NotBlank(message = "Seat code is required")
    private String seatCode;

    /**
     * Type of seat (PREMIUM, PLATINUM).
     */
    @Column(nullable = false)
    @NotNull(message = "Seat type is required")
    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    /**
     * Position index of the seat in the hall.
     */
    @Column(nullable = false)
    @NotNull(message = "Position index is required")
    private Integer positionIndex;

    /**
     * Lifecycle callback invoked before entity persistence.
     * Generates seat code from row label and seat number.
     */
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.generateSeatCode();
    }

    /**
     * Lifecycle callback invoked before entity update.
     * Regenerates seat code to reflect any changes to row label or seat number.
     */
    @PreUpdate
    @Override
    protected void onUpdate() {
        super.onUpdate();
        this.generateSeatCode();
    }

    /**
     * Generates seat code by concatenating row label and seat number.
     */
    private void generateSeatCode() {
        this.seatCode = this.rowLabel + this.seatNumber;
    }
}
