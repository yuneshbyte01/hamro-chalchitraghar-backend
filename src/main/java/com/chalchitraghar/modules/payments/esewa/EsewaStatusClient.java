package com.chalchitraghar.modules.payments.esewa;

import com.chalchitraghar.modules.payments.config.EsewaProperties;
import com.chalchitraghar.modules.payments.dto.esewa.EsewaStatusResponse;
import com.chalchitraghar.modules.payments.entity.Payment;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EsewaStatusClient {
    private final RestClient client;
    private final EsewaProperties p;
    private final EsewaSignatureService amounts;

    public EsewaStatusClient(
            RestClient esewaRestClient, EsewaProperties p, EsewaSignatureService amounts) {
        this.client = esewaRestClient;
        this.p = p;
        this.amounts = amounts;
    }

    public EsewaStatusResponse check(Payment payment) {
        return client.get()
                .uri(
                        p.statusCheckUrl().toString(),
                        b ->
                                b.queryParam("product_code", p.productCode())
                                        .queryParam(
                                                "total_amount", amounts.amount(payment.getAmount()))
                                        .queryParam(
                                                "transaction_uuid", payment.getPaymentReference())
                                        .build())
                .retrieve()
                .body(EsewaStatusResponse.class);
    }
}
