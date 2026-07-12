package com.chalchitraghar.modules.tickets.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import com.chalchitraghar.modules.tickets.entity.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {
    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.show", "booking.show.movie", "booking.show.hall", "bookingSeat", "bookingSeat.seat"})
    Optional<Ticket> findByTicketReference(String ticketReference);

    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.show", "booking.show.movie", "booking.show.hall", "bookingSeat", "bookingSeat.seat"})
    Optional<Ticket> findByTicketReferenceAndBookingUserId(String ticketReference, Long userId);

    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.show", "booking.show.movie", "booking.show.hall", "bookingSeat", "bookingSeat.seat"})
    List<Ticket> findByBookingIdOrderByIssuedAtAsc(Long bookingId);

    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.show", "booking.show.movie", "booking.show.hall", "bookingSeat", "bookingSeat.seat"})
    List<Ticket> findByBookingBookingReferenceOrderByIssuedAtAsc(String bookingReference);

    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.show", "booking.show.movie", "booking.show.hall", "bookingSeat", "bookingSeat.seat"})
    List<Ticket> findByBookingUserIdOrderByIssuedAtDesc(Long userId);

    boolean existsByTicketReference(String ticketReference);
    boolean existsByBookingSeatId(Long bookingSeatId);
    Optional<Ticket> findByBookingSeatId(Long bookingSeatId);
    Optional<Ticket> findByQrTokenHash(String qrTokenHash);
    boolean existsByQrTokenHash(String qrTokenHash);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"booking", "booking.user", "booking.show", "booking.show.movie", "booking.show.hall", "bookingSeat", "bookingSeat.seat"})
    @Query("select t from Ticket t where t.qrTokenHash=:hash")
    Optional<Ticket> findByQrTokenHashForUpdate(@Param("hash") String hash);
}
