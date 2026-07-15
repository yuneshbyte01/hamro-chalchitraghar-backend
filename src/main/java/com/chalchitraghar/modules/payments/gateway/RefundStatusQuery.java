package com.chalchitraghar.modules.payments.gateway;

import com.chalchitraghar.modules.payments.enums.PaymentProvider;

public record RefundStatusQuery(
        String refundReference, String providerRefundReference, PaymentProvider provider) {}
