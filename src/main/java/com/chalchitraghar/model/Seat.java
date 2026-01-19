package com.chalchitraghar.model;

import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import jakarta.validation.constraints.NotNull;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import com.chalchitraghar.model.enums.SeatType;
import com.chalchitraghar.model.enums.SeatStatus;
import java.time.LocalDateTime;

import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "seats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    @NotNull(message = "Created at is required")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @NotNull(message = "Updated at is required")
    private LocalDateTime updatedAt;

    @Column(nullable = true)
    private LocalDateTime lockedAt;

    @Column(nullable = true)
    private LocalDateTime lockExpiresAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.seatStatus = SeatStatus.AVAILABLE;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
