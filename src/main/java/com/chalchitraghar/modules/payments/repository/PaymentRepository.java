package com.chalchitraghar.modules.payments.repository;

import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.PaymentProvider;
import com.chalchitraghar.modules.payments.enums.PaymentStatus;
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

public interface PaymentRepository
        extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByPaymentReference(String paymentReference);

    @Query(
            "SELECT p FROM Payment p WHERE p.booking.id = :bookingId ORDER BY p.createdAt DESC, p.id DESC")
    List<Payment> findByBookingIdOrderByCreatedAtDesc(@Param("bookingId") Long bookingId);

    @Query(
            "SELECT p FROM Payment p WHERE p.booking.user.id = :userId ORDER BY p.createdAt DESC, p.id DESC")
    List<Payment> findByBookingUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    boolean existsByPaymentReference(String paymentReference);

    Optional<Payment> findByProviderAndProviderTransactionId(
            PaymentProvider provider, String providerTransactionId);

    boolean existsByProviderAndProviderTransactionId(
            PaymentProvider provider, String providerTransactionId);

    Optional<Payment> findByBookingIdAndIdempotencyKey(Long bookingId, String idempotencyKey);

    List<Payment> findByBookingIdAndStatusIn(
            Long bookingId, java.util.Collection<PaymentStatus> statuses);

    boolean existsByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    long countByBookingIdAndStatus(Long bookingId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
            "SELECT p FROM Payment p WHERE p.booking.id=:bookingId AND p.status=:status ORDER BY p.id")
    List<Payment> findByBookingIdAndStatusForUpdate(
            @Param("bookingId") Long bookingId, @Param("status") PaymentStatus status);

    java.util.Optional<Payment> findFirstByBookingIdAndStatusOrderByCompletedAtDesc(
            Long bookingId, PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.paymentReference = :reference")
    Optional<Payment> findByPaymentReferenceForUpdate(@Param("reference") String reference);

    Page<Payment> findByStatusOrderByCreatedAtAsc(PaymentStatus status, Pageable pageable);

    Page<Payment> findByStatusInAndExpiresAtBeforeOrderByExpiresAtAsc(
            java.util.Collection<PaymentStatus> statuses, LocalDateTime now, Pageable pageable);

    Page<Payment> findByManualReviewRequiredTrue(Pageable pageable);

    long countByStatus(PaymentStatus status);

    long countByManualReviewRequiredTrue();

    @Query(
            "SELECT COALESCE(SUM(p.amount),0), COALESCE(AVG(p.amount),0) FROM Payment p WHERE p.status='SUCCESS'")
    java.util.List<Object[]> successfulAmountStatistics();
}
