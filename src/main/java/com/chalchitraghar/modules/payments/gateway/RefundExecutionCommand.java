package com.chalchitraghar.modules.payments.gateway;

import com.chalchitraghar.modules.payments.enums.*;
import java.math.BigDecimal;

public record RefundExecutionCommand(
        String refundReference,
        String idempotencyKey,
        String paymentReference,
        String bookingReference,
        BigDecimal amount,
        String currency,
        PaymentProvider provider,
        RefundMethod method,
        String correlationId) {}
