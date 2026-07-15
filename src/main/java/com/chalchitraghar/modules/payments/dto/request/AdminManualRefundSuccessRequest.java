package com.chalchitraghar.modules.payments.dto.request;

import jakarta.validation.constraints.*;

public record AdminManualRefundSuccessRequest(
        @NotBlank @Size(max = 255) String externalReference, @Size(max = 500) String note) {}
