package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.payments.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Customer-safe payment summary")
public record CustomerPaymentSummaryResponse(
        String paymentReference,
        String bookingReference,
        PaymentProvider provider,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        LocalDateTime initiatedAt,
        LocalDateTime completedAt) {}
