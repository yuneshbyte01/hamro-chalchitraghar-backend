package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import com.chalchitraghar.modules.payments.repository.RefundRepository;
import com.chalchitraghar.shared.exception.PaymentConflictException;
import java.math.BigDecimal;
import java.util.EnumSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundBalanceService {
    public static final EnumSet<RefundStatus> RESERVING_STATUSES =
            EnumSet.of(
                    RefundStatus.REQUESTED,
                    RefundStatus.APPROVED,
                    RefundStatus.PROCESSING,
                    RefundStatus.SUCCEEDED,
                    RefundStatus.MANUAL_REVIEW);
    private final RefundRepository repository;

    public BigDecimal refundableBalance(Payment payment) {
        BigDecimal reserved =
                repository.sumAmountByPaymentIdAndStatusIn(payment.getId(), RESERVING_STATUSES);
        BigDecimal balance =
                payment.getAmount().subtract(reserved == null ? BigDecimal.ZERO : reserved);
        if (balance.signum() < 0)
            throw new PaymentConflictException("Refund reservations exceed payment amount");
        return balance;
    }
}
