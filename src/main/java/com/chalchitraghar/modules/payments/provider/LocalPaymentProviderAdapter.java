package com.chalchitraghar.modules.payments.provider;
import org.springframework.stereotype.Component;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.*;
@Component
public class LocalPaymentProviderAdapter implements PaymentProviderAdapter {
    public boolean supports(PaymentProvider provider) { return provider == PaymentProvider.LOCAL; }
    public void initiate(Payment payment) { payment.setStatus(PaymentStatus.PENDING); }
}
