package com.chalchitraghar.modules.payments.repository;

import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface RefundRepository
        extends JpaRepository<Refund, Long>, JpaSpecificationExecutor<Refund> {
    @EntityGraph(
            attributePaths = {
                "payment",
                "booking",
                "booking.user",
                "booking.show",
                "booking.show.movie",
                "booking.show.hall"
            })
    Optional<Refund> findByRefundReference(String refundReference);

    @EntityGraph(
            attributePaths = {
                "payment",
                "booking",
                "booking.user",
                "booking.show",
                "booking.show.movie",
                "booking.show.hall"
            })
    Optional<Refund> findByRefundReferenceAndBookingUserId(String refundReference, Long userId);

    @EntityGraph(
            attributePaths = {
                "payment",
                "booking",
                "booking.user",
                "booking.show",
                "booking.show.movie",
                "booking.show.hall"
            })
    Optional<Refund> findByIdempotencyKey(String idempotencyKey);

    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(
            attributePaths = {
                "payment",
                "booking",
                "booking.user",
                "booking.show",
                "booking.show.movie",
                "booking.show.hall"
            })
    @Query("select r from Refund r where r.refundReference=:reference")
    Optional<Refund> findByRefundReferenceForUpdate(@Param("reference") String reference);

    boolean existsByRefundReference(String refundReference);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @EntityGraph(
            attributePaths = {
                "payment",
                "booking",
                "booking.user",
                "booking.show",
                "booking.show.movie",
                "booking.show.hall"
            })
    List<Refund> findByBookingIdOrderByRequestedAtDescIdDesc(Long bookingId);

    List<Refund> findByPaymentIdOrderByRequestedAtDescIdDesc(Long paymentId);

    @Override
    @EntityGraph(
            attributePaths = {
                "payment",
                "booking",
                "booking.user",
                "booking.show",
                "booking.show.movie",
                "booking.show.hall"
            })
    Page<Refund> findAll(
            org.springframework.data.jpa.domain.Specification<Refund> spec, Pageable pageable);

    @Query(
            "select coalesce(sum(r.amount), 0) from Refund r where r.payment.id=:paymentId and r.status in :statuses")
    BigDecimal sumAmountByPaymentIdAndStatusIn(
            @Param("paymentId") Long paymentId,
            @Param("statuses") Collection<RefundStatus> statuses);

    List<Refund> findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
            RefundStatus status, LocalDateTime now);

    List<Refund> findTop50ByStatusAndClaimedAtBeforeOrderByClaimedAtAsc(
            RefundStatus status, LocalDateTime cutoff);

    @Query(
            "select r.status as status,r.currency as currency,count(r) as count,coalesce(sum(r.amount),0) as amount from Refund r where r.requestedAt between :from and :to group by r.status,r.currency order by r.currency,r.status")
    List<RefundAggregateProjection> aggregateStatus(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select r.reason as key,r.currency as currency,count(r) as count,coalesce(sum(r.amount),0) as amount from Refund r where r.requestedAt between :from and :to group by r.reason,r.currency order by r.currency,r.reason")
    List<RefundNamedAggregateProjection> aggregateReason(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select r.method as key,r.currency as currency,count(r) as count,coalesce(sum(r.amount),0) as amount from Refund r where r.requestedAt between :from and :to group by r.method,r.currency order by r.currency,r.method")
    List<RefundNamedAggregateProjection> aggregateMethod(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select r.provider as key,r.currency as currency,count(r) as count,coalesce(sum(r.amount),0) as amount from Refund r where r.requestedAt between :from and :to group by r.provider,r.currency order by r.currency,r.provider")
    List<RefundNamedAggregateProjection> aggregateProvider(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select coalesce(sum(case when r.attemptCount>1 then r.attemptCount-1 else 0 end),0) from Refund r where r.requestedAt between :from and :to")
    long countRetries(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select count(r) from Refund r where r.attemptCount>=r.maxAttempts and r.requestedAt between :from and :to")
    long countExhausted(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select count(distinct r.booking.id) from Refund r where r.status='SUCCEEDED' and r.requestedAt between :from and :to")
    long countDistinctSucceededBookings(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(
            "select count(distinct r.payment.id) from Refund r where r.status='SUCCEEDED' and r.requestedAt between :from and :to")
    long countDistinctSucceededPayments(
            @Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
