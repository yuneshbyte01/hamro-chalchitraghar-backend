package com.chalchitraghar.modules.halls.entity;

import com.chalchitraghar.modules.seats.enums.SeatType;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Template defining seat configuration for a hall, used to generate actual seats for shows. */
@Entity
@Table(
        name = "seat_templates",
        uniqueConstraints = {
            @jakarta.persistence.UniqueConstraint(
                    name = "uk_seat_templates_hall_code",
                    columnNames = {"hall_id", "seat_code"}),
            @jakarta.persistence.UniqueConstraint(
                    name = "uk_seat_templates_hall_row_number",
                    columnNames = {"hall_id", "row_label", "seat_number"}),
            @jakarta.persistence.UniqueConstraint(
                    name = "uk_seat_templates_hall_position",
                    columnNames = {"hall_id", "position_index"})
        })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class SeatTemplate extends GenericEntity {

    /** Hall associated with this seat template. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hall_id", nullable = false)
    @NotNull(message = "Hall is required")
    private Hall hall;

    /** Row label for this seat template. */
    @Column(nullable = false)
    @NotBlank(message = "Row label is required")
    @Pattern(
            regexp = "^[A-Z]{1,2}$",
            message = "Row label must contain one or two uppercase letters A-Z")
    @Size(max = 2, message = "Row label must not exceed 2 characters")
    private String rowLabel;

    /** Seat number for this seat template. */
    @Column(nullable = false)
    @NotNull(message = "Seat number is required")
    @Positive(message = "Seat number must be greater than 0")
    @Min(value = 1, message = "Seat number must be at least 1")
    @Max(value = 100, message = "Seat number must not exceed 100")
    private Integer seatNumber;

    /** Generated seat code combining row label and seat number. Automatically maintained. */
    @Column(nullable = false)
    @NotBlank(message = "Seat code is required")
    @Setter(AccessLevel.NONE)
    private String seatCode;

    /** Type of seat (PREMIUM, PLATINUM). */
    @Column(nullable = false)
    @NotNull(message = "Seat type is required")
    @Enumerated(EnumType.STRING)
    private SeatType seatType;

    /** Position index of the seat in the hall. */
    @Column(nullable = false)
    @NotNull(message = "Position index is required")
    private Integer positionIndex;

    /**
     * Lifecycle callback invoked before entity persistence. Generates seat code from row label and
     * seat number.
     */
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        this.generateSeatCode();
    }

    /**
     * Lifecycle callback invoked before entity update. Regenerates seat code to reflect any changes
     * to row label or seat number.
     */
    @PreUpdate
    @Override
    protected void onUpdate() {
        super.onUpdate();
        this.generateSeatCode();
    }

    /** Generates seat code by concatenating row label and seat number. */
    private void generateSeatCode() {
        this.seatCode = this.rowLabel + this.seatNumber;
    }

    /** Assigns the generated code using the same authoritative rule as persistence callbacks. */
    public void refreshSeatCode() {
        this.generateSeatCode();
    }

    /** Creates a generated template and derives its code from row and seat number. */
    public static SeatTemplate generated(
            Hall hall,
            String rowLabel,
            Integer seatNumber,
            SeatType seatType,
            Integer positionIndex) {
        SeatTemplate template = new SeatTemplate();
        template.setHall(hall);
        template.setRowLabel(rowLabel);
        template.setSeatNumber(seatNumber);
        template.setSeatType(seatType);
        template.setPositionIndex(positionIndex);
        template.refreshSeatCode();
        return template;
    }
}
