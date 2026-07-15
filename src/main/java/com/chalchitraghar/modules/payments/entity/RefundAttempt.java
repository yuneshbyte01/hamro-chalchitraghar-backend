package com.chalchitraghar.modules.payments.entity;

import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
        name = "refund_attempts",
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_refund_attempt_number",
                        columnNames = {"refund_id", "attempt_number"}))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundAttempt extends GenericEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "refund_id", nullable = false, updatable = false)
    private Refund refund;

    @Column(name = "attempt_number", nullable = false, updatable = false)
    private int attemptNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30, updatable = false)
    private RefundMethod method;

    @Enumerated(EnumType.STRING)
    @Column(length = 30, updatable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RefundAttemptStatus status;

    @Column(name = "started_at", nullable = false, updatable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Column(length = 255)
    private String providerRefundReference;

    @Column(length = 100)
    private String providerStatus;

    @Column(length = 100)
    private String failureCode;

    @Column(length = 500)
    private String failureReason;

    @Column(length = 100, updatable = false)
    private String correlationId;
}
