package com.chalchitraghar.modules.payments.entity;
import java.math.BigDecimal; import java.time.LocalDateTime; import jakarta.persistence.*; import lombok.*;
import com.chalchitraghar.modules.payments.enums.RefundStatus; import com.chalchitraghar.shared.GenericEntity;
@Entity @Table(name="refunds") @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Refund extends GenericEntity {
 @ManyToOne(fetch=FetchType.LAZY,optional=false) @JoinColumn(name="payment_id",nullable=false) private Payment payment;
 @Column(name="refund_reference",nullable=false,unique=true,updatable=false,length=50) private String refundReference;
 @Column(nullable=false,precision=12,scale=2) private BigDecimal amount;
 @Enumerated(EnumType.STRING) @Column(nullable=false,length=30) private RefundStatus status;
 @Column(name="provider_refund_id",length=255) private String providerRefundId;
 @Column(name="completed_at") private LocalDateTime completedAt;
}
