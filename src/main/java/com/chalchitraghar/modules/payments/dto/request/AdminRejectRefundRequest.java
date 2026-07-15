package com.chalchitraghar.modules.payments.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminRejectRefundRequest(
        @NotBlank @Size(max = 50) String reasonCode, @Size(max = 500) String note) {}
