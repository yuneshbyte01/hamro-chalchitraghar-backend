package com.chalchitraghar.modules.payments.esewa;

import com.chalchitraghar.modules.payments.config.EsewaProperties;
import com.chalchitraghar.modules.payments.dto.esewa.EsewaStatusResponse;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.shared.observability.ProviderFailureCategory;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class EsewaStatusClient {
    private static final Logger log = LoggerFactory.getLogger(EsewaStatusClient.class);
    private static final String TIMER = "chalchitraghar.payment.provider.duration";

    private final RestClient client;
    private final EsewaProperties p;
    private final EsewaSignatureService amounts;
    private final MeterRegistry registry;

    public EsewaStatusClient(
            RestClient esewaRestClient,
            EsewaProperties p,
            EsewaSignatureService amounts,
            MeterRegistry registry) {
        this.client = esewaRestClient;
        this.p = p;
        this.amounts = amounts;
        this.registry = registry;
    }

    public EsewaStatusResponse check(Payment payment) {
        Timer.Sample sample = Timer.start(registry);
        try {
            EsewaStatusResponse response =
                    client.get()
                            .uri(
                                    p.statusCheckUrl().toString(),
                                    b ->
                                            b.queryParam("product_code", p.productCode())
                                                    .queryParam(
                                                            "total_amount",
                                                            amounts.amount(payment.getAmount()))
                                                    .queryParam(
                                                            "transaction_uuid",
                                                            payment.getPaymentReference())
                                                    .build())
                            .retrieve()
                            .body(EsewaStatusResponse.class);
            long duration = stop(sample, "success");
            log.atInfo()
                    .addKeyValue("event", "payment.provider.result")
                    .addKeyValue("provider", "esewa")
                    .addKeyValue("operation", "status_enquiry")
                    .addKeyValue("outcome", "success")
                    .addKeyValue("durationMs", duration)
                    .log("Payment provider status enquiry completed");
            return response;
        } catch (RuntimeException failure) {
            ProviderFailureCategory category = ProviderFailureCategory.classify(failure);
            long duration = stop(sample, "failure");
            log.atWarn()
                    .addKeyValue("event", "payment.provider.result")
                    .addKeyValue("provider", "esewa")
                    .addKeyValue("operation", "status_enquiry")
                    .addKeyValue("outcome", "failure")
                    .addKeyValue("failureCode", category.tag())
                    .addKeyValue("durationMs", duration)
                    .log("Payment provider status enquiry failed");
            throw failure;
        }
    }

    private long stop(Timer.Sample sample, String outcome) {
        long nanos =
                sample.stop(
                        Timer.builder(TIMER)
                                .description("Latency of outbound payment-provider operations")
                                .tag("provider", "esewa")
                                .tag("operation", "status_enquiry")
                                .tag("outcome", outcome)
                                .register(registry));
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }
}
