package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.gateway.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundProcessor {
    private final RefundClaimService claims;
    private final List<RefundGateway> gateways;
    private final RefundProcessingFinalizer finalizer;

    public Refund process(String reference) {
        return process(reference, false);
    }

    public Refund retry(String reference) {
        return process(reference, true);
    }

    private Refund process(String reference, boolean retry) {
        RefundProcessingClaim claim = claims.claim(reference, retry);
        RefundGateway gateway =
                gateways.stream()
                        .filter(g -> g.supports(claim.command().provider()))
                        .findFirst()
                        .orElseThrow();
        RefundGatewayResult result;
        try {
            result = gateway.execute(claim.command());
        } catch (RuntimeException ignored) {
            result =
                    new RefundGatewayResult(
                            RefundGatewayOutcome.UNKNOWN,
                            null,
                            null,
                            "PROVIDER_STATUS_UNKNOWN",
                            "Provider outcome requires review");
        }
        return finalizer.finalizeResult(claim.refundId(), claim.attemptNumber(), result);
    }
}
