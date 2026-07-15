package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.config.RefundProcessingProperties;
import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.enums.RefundAttemptStatus;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import com.chalchitraghar.modules.payments.repository.RefundAttemptRepository;
import com.chalchitraghar.modules.payments.repository.RefundRepository;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class StaleRefundRecoveryService {
    private final RefundRepository refunds;
    private final RefundProcessingProperties properties;
    private final Clock clock;
    private final RefundAttemptRepository attempts;

    @Transactional
    public int recover() {
        LocalDateTime now = LocalDateTime.now(clock);
        int count = 0;
        for (Refund candidate :
                refunds.findTop50ByStatusAndClaimedAtBeforeOrderByClaimedAtAsc(
                        RefundStatus.PROCESSING, now.minus(properties.getProcessingTimeout()))) {
            Refund r =
                    refunds.findByRefundReferenceForUpdate(candidate.getRefundReference())
                            .orElseThrow();
            if (r.getStatus() != RefundStatus.PROCESSING
                    || r.getClaimedAt() == null
                    || !r.getClaimedAt().isBefore(now.minus(properties.getProcessingTimeout())))
                continue;
            r.setStatus(RefundStatus.MANUAL_REVIEW);
            r.setManualReviewRequired(true);
            r.setLastFailureCode("PROCESSING_TIMEOUT");
            r.setFailureReason("Processing outcome requires review");
            r.setClaimedAt(null);
            r.setClaimedBy(null);
            r.setNextAttemptAt(null);
            r.setUpdatedAt(now);
            attempts.findForUpdate(r.getId(), r.getAttemptCount())
                    .ifPresent(
                            a -> {
                                a.setStatus(RefundAttemptStatus.UNKNOWN);
                                a.setCompletedAt(now);
                                a.setFailureCode("PROCESSING_TIMEOUT");
                                a.setFailureReason("Processing outcome requires review");
                                a.setUpdatedAt(now);
                            });
            count++;
        }
        return count;
    }
}
