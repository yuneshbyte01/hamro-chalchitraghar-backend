package com.chalchitraghar.modules.bookings.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;

/**
 * Repository interface for Booking entity persistence operations.
 */
public interface BookingRepository extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    boolean existsByShowId(Long showId);

    boolean existsByShowIdAndStatusIn(Long showId, List<BookingStatus> statuses);

    long countByShowIdAndStatusIn(Long showId, List<BookingStatus> statuses);
    
    /**
     * Finds all bookings for a specific user, ordered by booking time descending (most recent first).
     * 
     * @param userId the user ID
     * @return list of bookings for the user, sorted by bookingTime descending
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.bookingTime DESC")
    List<Booking> findByUserIdOrderByBookingTimeDesc(@Param("userId") Long userId);
}
