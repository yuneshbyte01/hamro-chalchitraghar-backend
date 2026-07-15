package com.chalchitraghar.modules.payments.entity;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refund extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false, updatable = false)
    private Payment payment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false, updatable = false)
    private Booking booking;

    @Column(
            name = "refund_reference",
            nullable = false,
            unique = true,
            updatable = false,
            length = 50)
    private String refundReference;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(nullable = false, length = 10, updatable = false)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50, updatable = false)
    private RefundReason reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_type", nullable = false, length = 30, updatable = false)
    private RefundType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 30, updatable = false)
    private RefundMethod method;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 200,
            updatable = false)
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", updatable = false)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by_user_id")
    private User approvedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rejected_by_user_id")
    private User rejectedBy;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "rejected_at")
    private LocalDateTime rejectedAt;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, updatable = false)
    private PaymentProvider provider;

    @Column(name = "provider_refund_reference", length = 255)
    private String providerRefundReference;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "rejection_reason_code", length = 50)
    private String rejectionReasonCode;

    @Column(name = "rejection_note", length = 500)
    private String rejectionNote;

    /** Legacy V21 column retained for forward migration compatibility. */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
