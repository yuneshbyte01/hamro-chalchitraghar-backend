package com.chalchitraghar.modules.bookings.repository;

import com.chalchitraghar.modules.bookings.entity.BookingSeat;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Repository interface for BookingSeat entity persistence operations. */
public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    /**
     * Checks if any of the given seats are already booked.
     *
     * @param seatIds list of seat IDs to check
     * @return list of BookingSeat records for seats that are already booked
     */
    @Query("SELECT bs FROM BookingSeat bs WHERE bs.seat.id IN :seatIds")
    List<BookingSeat> findBySeatIds(@Param("seatIds") List<Long> seatIds);

    /**
     * Finds BookingSeat records for bookings that still hold a seat claim.
     *
     * @param seatIds list of seat IDs to check
     * @return list of active BookingSeat records
     */
    @Query(
            "SELECT bs FROM BookingSeat bs WHERE bs.seat.id IN :seatIds "
                    + "AND bs.booking.status NOT IN ('CANCELLED', 'EXPIRED')")
    List<BookingSeat> findActiveBookingsBySeatIds(@Param("seatIds") List<Long> seatIds);

    /**
     * Finds all BookingSeat records for a specific booking.
     *
     * @param bookingId the booking ID
     * @return list of BookingSeat records for the booking
     */
    @Query("SELECT bs FROM BookingSeat bs WHERE bs.booking.id = :bookingId")
    List<BookingSeat> findByBookingId(@Param("bookingId") Long bookingId);

    @Query(
            "SELECT bs FROM BookingSeat bs JOIN FETCH bs.seat s "
                    + "WHERE bs.booking.id IN :bookingIds ORDER BY bs.booking.id, s.positionIndex")
    List<BookingSeat> findByBookingIdsWithSeats(@Param("bookingIds") List<Long> bookingIds);

    /**
     * Finds all BookingSeat records for seats that are part of CONFIRMED bookings. Excludes the
     * specified booking ID.
     *
     * @param seatIds list of seat IDs to check
     * @param excludeBookingId booking ID to exclude from the check
     * @return list of BookingSeat records for seats in CONFIRMED bookings
     */
    @Query(
            "SELECT bs FROM BookingSeat bs WHERE bs.seat.id IN :seatIds "
                    + "AND bs.booking.status = 'CONFIRMED' AND bs.booking.id != :excludeBookingId")
    List<BookingSeat> findConfirmedBookingsBySeatIds(
            @Param("seatIds") List<Long> seatIds, @Param("excludeBookingId") Long excludeBookingId);
}
