package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.payments.enums.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Admin payment summary")
public record AdminPaymentSummaryResponse(
        String paymentReference,
        String bookingReference,
        Long customerId,
        String customerName,
        String customerEmail,
        PaymentProvider provider,
        PaymentMethod method,
        PaymentStatus status,
        BigDecimal amount,
        String currency,
        LocalDateTime initiatedAt,
        LocalDateTime completedAt) {}
