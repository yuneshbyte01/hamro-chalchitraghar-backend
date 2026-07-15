package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.gateway.RefundExecutionCommand;

public record RefundProcessingClaim(
        Long refundId, int attemptNumber, RefundExecutionCommand command) {}
