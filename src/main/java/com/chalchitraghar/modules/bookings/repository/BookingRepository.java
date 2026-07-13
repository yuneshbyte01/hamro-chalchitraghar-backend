package com.chalchitraghar.modules.bookings.repository;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository interface for Booking entity persistence operations. */
public interface BookingRepository
        extends JpaRepository<Booking, Long>, JpaSpecificationExecutor<Booking> {

    boolean existsByShowId(Long showId);

    boolean existsByBookingReference(String bookingReference);

    Optional<Booking> findByBookingReference(String bookingReference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Booking b WHERE b.id = :bookingId")
    Optional<Booking> findByIdForUpdate(@Param("bookingId") Long bookingId);

    @Query(
            "SELECT b FROM Booking b WHERE b.status = 'INITIATED' AND b.expiresAt <= :now ORDER BY b.expiresAt, b.id")
    Page<Booking> findExpiredInitiatedBookings(@Param("now") LocalDateTime now, Pageable pageable);

    List<Booking> findByShowIdAndStatus(Long showId, BookingStatus status);

    boolean existsByShowIdAndStatusIn(Long showId, List<BookingStatus> statuses);

    long countByShowIdAndStatusIn(Long showId, List<BookingStatus> statuses);

    /**
     * Finds all bookings for a specific user, ordered by booking time descending (most recent
     * first).
     *
     * @param userId the user ID
     * @return list of bookings for the user, sorted by bookingTime descending
     */
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId ORDER BY b.bookingTime DESC")
    List<Booking> findByUserIdOrderByBookingTimeDesc(@Param("userId") Long userId);
}
