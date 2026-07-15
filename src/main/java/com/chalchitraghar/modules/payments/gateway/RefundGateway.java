package com.chalchitraghar.modules.payments.gateway;

import com.chalchitraghar.modules.payments.enums.PaymentProvider;

public interface RefundGateway {
    boolean supports(PaymentProvider provider);

    RefundGatewayResult execute(RefundExecutionCommand command);

    RefundGatewayResult queryStatus(RefundStatusQuery query);
}
