package com.chalchitraghar.modules.payments.dto.response;
import java.math.BigDecimal; import com.chalchitraghar.modules.payments.enums.RefundStatus;
public record RefundResponse(String refundReference,String paymentReference,BigDecimal amount,RefundStatus status) {}
