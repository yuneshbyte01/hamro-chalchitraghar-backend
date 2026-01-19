package com.chalchitraghar.model;

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
import com.chalchitraghar.model.enums.SeatType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

@Entity
@Table(name = "seat_templates")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatTemplate extends GenericEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    @NotNull(message = "Hall is required")
    private Hall hall;

    @Column(nullable = false)
    @NotBlank(message = "Row label is required")
    private String rowLabel;

    @Column(nullable = false)
    @NotNull(message = "Seat number is required")
    @Positive(message = "Seat number must be greater than 0")
    private Integer seatNumber;

    @Column(nullable = false)
    @NotBlank(message = "Seat code is required")
    private String seatCode;

    @Column(nullable = false)
    @NotNull(message = "Seat type is required")
    @Enumerated(EnumType.STRING)
    private SeatType seatType; // PREMIUM, PLATINUM

    @Column(nullable = false)
    @NotNull(message = "Position index is required")
    private Integer positionIndex;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.generateSeatCode();
    }

    @PreUpdate
    @Override
    protected void onUpdate() {
        super.onUpdate();
        this.generateSeatCode();
    }

    private void generateSeatCode() {
        this.seatCode = this.rowLabel + this.seatNumber;
    }
}
