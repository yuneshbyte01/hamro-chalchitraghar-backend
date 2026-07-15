package com.chalchitraghar.modules.payments.dto.request;

import com.chalchitraghar.modules.payments.enums.RefundReason;

/** Trusted internal command. It is intentionally not accepted by an HTTP controller. */
public record CreateRefundIntentCommand(
        String paymentReference,
        RefundReason reason,
        String idempotencyKey,
        Long requestedByUserId) {}
