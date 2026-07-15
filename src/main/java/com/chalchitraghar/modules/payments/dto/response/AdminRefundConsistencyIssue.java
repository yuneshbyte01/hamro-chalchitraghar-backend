package com.chalchitraghar.modules.payments.dto.response;

public record AdminRefundConsistencyIssue(
        String code,
        String severity,
        String description,
        String affectedResource,
        boolean repairable) {}
