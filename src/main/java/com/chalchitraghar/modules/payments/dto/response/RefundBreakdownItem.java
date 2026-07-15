package com.chalchitraghar.modules.payments.dto.response;

import java.math.BigDecimal;

public record RefundBreakdownItem(String key, long count, BigDecimal amount, String currency) {}
