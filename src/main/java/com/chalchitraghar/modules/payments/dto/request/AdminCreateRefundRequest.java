package com.chalchitraghar.modules.payments.dto.request;

import com.chalchitraghar.modules.payments.enums.RefundReason;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminCreateRefundRequest(
        @NotBlank String paymentReference, @NotNull RefundReason reason) {}
