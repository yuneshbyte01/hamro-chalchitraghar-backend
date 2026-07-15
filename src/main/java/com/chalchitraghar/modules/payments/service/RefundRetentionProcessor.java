package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.config.RefundRetentionProperties;
import com.chalchitraghar.modules.payments.entity.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.repository.*;
import java.time.*;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefundRetentionProcessor {
    private static final Set<RefundStatus> TERMINAL =
            Set.of(RefundStatus.SUCCEEDED, RefundStatus.REJECTED, RefundStatus.FAILED);
    private final RefundRepository refunds;
    private final RefundAttemptRepository attempts;
    private final RefundRetentionProperties properties;
    private final Clock clock;

    @Transactional
    public int processBatch() {
        if (!properties.isEnabled()) return 0;
        LocalDateTime now = LocalDateTime.now(clock);
        int size = properties.getBatchSize();
        Specification<Refund> spec =
                (root, q, cb) ->
                        cb.and(
                                root.get("status").in(TERMINAL),
                                cb.isNull(root.get("anonymizedAt")),
                                cb.lessThan(
                                        root.get("requestedAt"),
                                        now.minusDays(properties.getRefundDays())),
                                cb.or(
                                        cb.notEqual(root.get("status"), RefundStatus.FAILED),
                                        cb.isNull(root.get("nextAttemptAt"))));
        var page =
                refunds.findAll(
                        spec,
                        PageRequest.of(
                                0, size, Sort.by("requestedAt").ascending().and(Sort.by("id"))));
        page.forEach(
                r -> {
                    r.setRejectionNote(null);
                    r.setFailureReason(
                            r.getFailureReason() == null ? null : "Archived refund outcome");
                    r.setClaimedBy(null);
                    r.setRetentionStatus(RefundRetentionStatus.ANONYMIZED);
                    r.setAnonymizedAt(now);
                    r.setUpdatedAt(now);
                });
        int count = page.getNumberOfElements();
        int remaining = size - count;
        if (remaining > 0) {
            var old =
                    attempts.findRetentionCandidates(
                            now.minusDays(properties.getAttemptDays()),
                            PageRequest.of(0, remaining));
            old.forEach(
                    a -> {
                        a.setFailureReason(
                                a.getFailureReason() == null ? null : "Archived attempt outcome");
                        a.setCorrelationId(null);
                        a.setAnonymizedAt(now);
                        a.setUpdatedAt(now);
                    });
            count += old.getNumberOfElements();
        }
        return count;
    }
}
