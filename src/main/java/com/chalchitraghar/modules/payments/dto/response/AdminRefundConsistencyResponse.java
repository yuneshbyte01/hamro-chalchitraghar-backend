package com.chalchitraghar.modules.payments.dto.response;

import java.time.LocalDateTime;
import java.util.List;

public record AdminRefundConsistencyResponse(
        String refundReference,
        boolean consistent,
        int issueCount,
        List<AdminRefundConsistencyIssue> issues,
        LocalDateTime checkedAt) {}
