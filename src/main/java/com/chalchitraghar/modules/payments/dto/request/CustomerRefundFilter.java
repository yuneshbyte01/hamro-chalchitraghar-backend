package com.chalchitraghar.modules.payments.dto.request;

import com.chalchitraghar.modules.payments.enums.RefundReason;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import java.time.LocalDateTime;

public record CustomerRefundFilter(
        RefundStatus status,
        RefundReason reason,
        LocalDateTime requestedFrom,
        LocalDateTime requestedTo) {}
