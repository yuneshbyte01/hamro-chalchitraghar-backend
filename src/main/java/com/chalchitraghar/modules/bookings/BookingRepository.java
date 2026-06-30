package com.chalchitraghar.modules.bookings;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chalchitraghar.modules.bookings.Booking;

/**
 * Repository interface for Booking entity persistence operations.
 */
public interface BookingRepository extends JpaRepository<Booking, Long> {
    
    /**
     * Finds all bookings for a specific user, ordered by booking time descending (most recent first).
     * 
     * @param userId the user ID
     * @return list of bookings for the user, sorted by bookingTime descending
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.bookingTime DESC")
    List<Booking> findByUserIdOrderByBookingTimeDesc(@Param("userId") Long userId);
}