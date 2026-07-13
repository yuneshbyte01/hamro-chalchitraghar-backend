package com.chalchitraghar.modules.payments.mapper;

import com.chalchitraghar.modules.payments.dto.response.RefundResponse;
import com.chalchitraghar.modules.payments.entity.Refund;
import org.springframework.stereotype.Component;

@Component
public class RefundMapper {
    public RefundResponse toResponse(Refund r) {
        return new RefundResponse(
                r.getRefundReference(),
                r.getPayment().getPaymentReference(),
                r.getAmount(),
                r.getStatus());
    }
}
