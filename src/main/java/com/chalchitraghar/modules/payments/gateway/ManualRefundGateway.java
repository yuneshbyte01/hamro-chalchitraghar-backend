package com.chalchitraghar.modules.payments.gateway;

import com.chalchitraghar.modules.payments.enums.PaymentProvider;
import org.springframework.stereotype.Component;

@Component
public class ManualRefundGateway implements RefundGateway {
    public boolean supports(PaymentProvider provider) {
        return true;
    }

    public RefundGatewayResult execute(RefundExecutionCommand command) {
        return RefundGatewayResult.manualReview("MANUAL_CONFIRMATION_REQUIRED");
    }

    public RefundGatewayResult queryStatus(RefundStatusQuery query) {
        return RefundGatewayResult.manualReview("UNSUPPORTED_PROVIDER");
    }
}
