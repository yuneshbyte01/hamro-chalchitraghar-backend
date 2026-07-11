package com.chalchitraghar.modules.payments.dto.response;
import java.math.BigDecimal; import java.time.LocalDateTime;
import com.chalchitraghar.modules.payments.enums.*; import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description="Customer-safe payment detail")
public record CustomerPaymentDetailResponse(String paymentReference, String bookingReference, PaymentProvider provider,
        PaymentMethod method, PaymentStatus status, BigDecimal amount, String currency, String providerTransactionId,
        LocalDateTime initiatedAt, LocalDateTime expiresAt, LocalDateTime completedAt, LocalDateTime failedAt, LocalDateTime expiredAt,
        LocalDateTime cancelledAt, String failureMessage) {}
