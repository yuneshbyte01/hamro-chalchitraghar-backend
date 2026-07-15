package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.entity.Refund;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import com.chalchitraghar.modules.payments.repository.RefundRepository;
import com.chalchitraghar.shared.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundReconciliationService {
    private final RefundRepository refunds;

    public Refund reconcile(String reference) {
        Refund r =
                refunds.findByRefundReference(reference.trim().toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Refund not found"));
        if (r.getStatus() != RefundStatus.MANUAL_REVIEW)
            throw new PaymentConflictException("Only manual-review refunds can be reconciled");
        throw new PaymentConflictException(
                "Provider refund status enquiry is not supported; use manual resolution");
    }
}
