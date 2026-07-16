package com.chalchitraghar.modules.reporting.projection;

import java.math.BigDecimal;

public interface PaymentRevenueProjection {
    String getCurrency();

    long getCount();

    BigDecimal getAmount();
}
