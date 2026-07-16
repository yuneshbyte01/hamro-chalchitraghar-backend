package com.chalchitraghar.modules.reporting.projection;

import java.math.BigDecimal;

public interface RefundRevenueProjection {
    String getCurrency();

    long getCount();

    BigDecimal getAmount();
}
