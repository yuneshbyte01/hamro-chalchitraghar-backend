package com.chalchitraghar.modules.reporting.dto.response;

import java.util.List;

public record AdminRevenueKpiResponse(
        ReportingPeriodResponse period, List<CurrencyRevenueKpiResponse> currencies) {}
