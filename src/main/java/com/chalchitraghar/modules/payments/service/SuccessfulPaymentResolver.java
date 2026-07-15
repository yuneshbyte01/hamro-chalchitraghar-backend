package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.PaymentStatus;
import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.shared.exception.PaymentConflictException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuccessfulPaymentResolver {
    private final PaymentRepository payments;

    /** Locks successful payment rows before booking/ticket mutation. */
    public Payment resolveForUpdate(Long bookingId) {
        var successful =
                payments.findByBookingIdAndStatusForUpdate(bookingId, PaymentStatus.SUCCESS);
        if (successful.isEmpty())
            throw new PaymentConflictException("A successful payment is required for refund");
        if (successful.size() != 1)
            throw new PaymentConflictException(
                    "Multiple successful payments require manual financial review");
        return successful.getFirst();
    }
}
