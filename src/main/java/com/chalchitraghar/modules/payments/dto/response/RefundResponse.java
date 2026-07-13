package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.payments.enums.RefundStatus;
import java.math.BigDecimal;

public record RefundResponse(
        String refundReference, String paymentReference, BigDecimal amount, RefundStatus status) {}
