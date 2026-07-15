package com.chalchitraghar.modules.payments.repository;

import com.chalchitraghar.modules.payments.enums.RefundStatus;
import java.math.BigDecimal;

public interface RefundAggregateProjection {
    RefundStatus getStatus();

    String getCurrency();

    long getCount();

    BigDecimal getAmount();
}
