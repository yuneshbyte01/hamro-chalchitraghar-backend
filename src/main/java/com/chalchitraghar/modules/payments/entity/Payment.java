package com.chalchitraghar.modules.payments.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.payments.enums.PaymentMethod;
import com.chalchitraghar.modules.payments.enums.PaymentProvider;
import com.chalchitraghar.modules.payments.enums.PaymentStatus;
import com.chalchitraghar.shared.GenericEntity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "payments", uniqueConstraints = @UniqueConstraint(name = "uk_payments_reference", columnNames = "payment_reference"))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Payment extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    @NotNull private Booking booking;

    @Column(name = "payment_reference", nullable = false, unique = true, updatable = false, length = 50)
    @NotNull @Size(max = 50) private String paymentReference;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    @NotNull private PaymentProvider provider;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    @NotNull private PaymentMethod method;

    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    @NotNull private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    @NotNull @DecimalMin(value = "0.01") private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @NotNull @Size(min = 3, max = 3) private String currency;

    @Column(name = "provider_transaction_id", length = 255) private String providerTransactionId;
    @Column(name = "idempotency_key", length = 255) private String idempotencyKey;
    @Column(name = "failure_code", length = 100) private String failureCode;
    @Column(name = "failure_message", length = 500) private String failureMessage;
    @Column(name = "initiated_at") private LocalDateTime initiatedAt;
    @Column(name = "completed_at") private LocalDateTime completedAt;
    @Column(name = "failed_at") private LocalDateTime failedAt;
    @Column(name = "expired_at") private LocalDateTime expiredAt;
    @Column(name = "cancelled_at") private LocalDateTime cancelledAt;
}
