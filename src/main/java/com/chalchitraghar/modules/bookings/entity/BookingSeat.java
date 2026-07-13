package com.chalchitraghar.modules.bookings.entity;

import com.chalchitraghar.modules.seats.entity.Seat;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents the many-to-many relationship between Booking and Seat entities. Maps which seats are
 * included in a booking.
 */
@Entity
@Table(
        name = "booking_seats",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_booking_seats_booking_seat",
                        columnNames = {"booking_id", "seat_id"}))
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat extends GenericEntity {

    /** Booking that includes this seat. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @NotNull(message = "Booking is required")
    private Booking booking;

    /** Seat included in this booking. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    @NotNull(message = "Seat is required")
    private Seat seat;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    @NotNull(message = "Unit price is required")
    private BigDecimal unitPrice;
}
