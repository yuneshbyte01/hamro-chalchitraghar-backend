package com.chalchitraghar.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import java.time.LocalDateTime;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import com.chalchitraghar.model.enums.SeatType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;

@Entity
@Table(name = "seat_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Column(nullable = false)
    @NotNull(message = "Created at is required")
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @NotNull(message = "Updated at is required")
    private LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.generateSeatCode();
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        this.generateSeatCode();
    }

    private void generateSeatCode() {
        this.seatCode = this.rowLabel + this.seatNumber;
    }
}
