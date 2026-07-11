package com.chalchitraghar.modules.payments.provider;
import org.springframework.stereotype.Component; import com.chalchitraghar.modules.payments.entity.Payment; import com.chalchitraghar.modules.payments.enums.*;
@Component public class EsewaPaymentProviderAdapter implements PaymentProviderAdapter {
 public boolean supports(PaymentProvider provider){return provider==PaymentProvider.ESEWA;}
 public void initiate(Payment payment){if(!"NPR".equals(payment.getCurrency()))throw new IllegalArgumentException("eSewa supports NPR payments only");payment.setStatus(PaymentStatus.PENDING);}
}
