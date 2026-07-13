package com.chalchitraghar.modules.payments.dto.esewa;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;

public record EsewaStatusResponse(
        @JsonProperty("product_code") String productCode,
        @JsonProperty("transaction_uuid") String transactionUuid,
        @JsonProperty("total_amount") BigDecimal totalAmount,
        String status,
        @JsonProperty("ref_id") String refId) {}
