package com.chalchitraghar.model;

import java.time.LocalDateTime;

import com.chalchitraghar.model.enums.SeatStatus;
import com.chalchitraghar.model.enums.SeatType;

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

@Entity
@Table(name = "seats")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat extends GenericEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    @NotNull(message = "Show is required")
    private Show show;

    @Column(nullable = false)
    @NotNull(message = "Seat number is required")
    private Integer seatNumber;

    @Column(nullable = false)
    @NotNull(message = "Row label is required")
    private String rowLabel;

    @Column(nullable = false)
    @NotNull(message = "Seat code is required")
    private String seatCode;

    @Column(nullable = false)
    @NotNull(message = "Seat type is required")
    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    @Column(nullable = false)
    @NotNull(message = "Seat status is required")
    @Enumerated(EnumType.STRING)
    private SeatStatus seatStatus;

    @Column(nullable = false)
    @NotNull(message = "Position index is required")
    private Integer positionIndex;

    @Column(nullable = true)
    private LocalDateTime lockedAt;

    @Column(nullable = true)
    private LocalDateTime lockExpiresAt;

    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.seatStatus = SeatStatus.AVAILABLE;
    }
}
