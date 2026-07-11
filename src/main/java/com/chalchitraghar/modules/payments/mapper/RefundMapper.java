package com.chalchitraghar.modules.payments.mapper;
import org.springframework.stereotype.Component; import com.chalchitraghar.modules.payments.entity.Refund; import com.chalchitraghar.modules.payments.dto.response.RefundResponse;
@Component public class RefundMapper { public RefundResponse toResponse(Refund r){return new RefundResponse(r.getRefundReference(),r.getPayment().getPaymentReference(),r.getAmount(),r.getStatus());} }
