package com.chalchitraghar.modules.payments.repository;

import java.math.BigDecimal;

public interface RefundNamedAggregateProjection {
    Object getKey();

    String getCurrency();

    long getCount();

    BigDecimal getAmount();
}
