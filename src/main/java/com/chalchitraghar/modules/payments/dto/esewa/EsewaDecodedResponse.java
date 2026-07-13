package com.chalchitraghar.modules.payments.dto.esewa;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EsewaDecodedResponse(
        @JsonProperty("transaction_code") String transactionCode,
        String status,
        @JsonProperty("total_amount") String totalAmount,
        @JsonProperty("transaction_uuid") String transactionUuid,
        @JsonProperty("product_code") String productCode,
        @JsonProperty("signed_field_names") String signedFieldNames,
        String signature) {}
