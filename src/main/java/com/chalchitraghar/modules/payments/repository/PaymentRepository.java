package com.chalchitraghar.modules.payments.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.PaymentProvider;

public interface PaymentRepository extends JpaRepository<Payment, Long>, JpaSpecificationExecutor<Payment> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    @Query("SELECT p FROM Payment p WHERE p.booking.id = :bookingId ORDER BY p.createdAt DESC, p.id DESC")
    List<Payment> findByBookingIdOrderByCreatedAtDesc(@Param("bookingId") Long bookingId);
    @Query("SELECT p FROM Payment p WHERE p.booking.user.id = :userId ORDER BY p.createdAt DESC, p.id DESC")
    List<Payment> findByBookingUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);
    boolean existsByPaymentReference(String paymentReference);
    Optional<Payment> findByProviderAndProviderTransactionId(PaymentProvider provider, String providerTransactionId);
}
