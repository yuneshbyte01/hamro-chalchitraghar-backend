package com.chalchitraghar.modules.bookings;

import com.chalchitraghar.modules.shows.Show;

import com.chalchitraghar.modules.users.User;

import com.chalchitraghar.shared.GenericEntity;

import java.time.LocalDateTime;

import com.chalchitraghar.modules.bookings.BookingStatus;

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
 * Represents a booking made by a user for a show with selected seats.
 */
@Entity
@Table(name = "bookings")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking extends GenericEntity {

    /**
     * User who made this booking.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @NotNull(message = "User is required")
    private User user;

    /**
     * Show for which this booking is made.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "show_id", nullable = false)
    @NotNull(message = "Show is required")
    private Show show;

    /**
     * Timestamp when the booking was created.
     */
    @Column(nullable = false)
    @NotNull(message = "Booking time is required")
    private LocalDateTime bookingTime;

    /**
     * Current status of the booking.
     */
    @Column(nullable = false)
    @NotNull(message = "Booking status is required")
    @Enumerated(EnumType.STRING)
    private BookingStatus status;

    /**
     * Lifecycle callback invoked before entity persistence.
     * Sets default status to INITIATED and bookingTime to current timestamp.
     */
    @PrePersist
    @Override
    protected void onCreate() {
        super.onCreate();
        if (this.status == null) {
            this.status = BookingStatus.INITIATED;
        }
        if (this.bookingTime == null) {
            this.bookingTime = LocalDateTime.now();
        }
    }
}
