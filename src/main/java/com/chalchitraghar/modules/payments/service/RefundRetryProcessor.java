package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import com.chalchitraghar.modules.payments.repository.RefundRepository;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundRetryProcessor {
    private final RefundRepository refunds;
    private final RefundProcessor processor;
    private final Clock clock;

    public int processBatch() {
        int n = 0;
        for (Refund r :
                refunds.findTop50ByStatusAndNextAttemptAtLessThanEqualOrderByNextAttemptAtAsc(
                        RefundStatus.FAILED, LocalDateTime.now(clock))) {
            try {
                processor.process(r.getRefundReference());
                n++;
            } catch (RuntimeException ignored) {
            }
        }
        return n;
    }
}
