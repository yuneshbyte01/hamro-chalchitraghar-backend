package com.chalchitraghar.modules.payments.provider;

import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.PaymentProvider;

public interface PaymentProviderAdapter {
    boolean supports(PaymentProvider provider);

    void initiate(Payment payment);
}
