package com.chalchitraghar.modules.payments.dto.response;
import java.math.BigDecimal; import java.time.LocalDateTime;
import com.chalchitraghar.modules.payments.enums.*; import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description="Admin payment detail")
public record AdminPaymentDetailResponse(String paymentReference, String bookingReference, Long customerId,
        String customerName, String customerEmail, PaymentProvider provider, PaymentMethod method, PaymentStatus status,
        BigDecimal amount, String currency, LocalDateTime initiatedAt, LocalDateTime completedAt,
        String providerTransactionId, String failureCode, String failureMessage, LocalDateTime failedAt,
        LocalDateTime expiredAt, LocalDateTime cancelledAt, LocalDateTime createdAt, LocalDateTime updatedAt) {}
