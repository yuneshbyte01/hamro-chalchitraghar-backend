package com.chalchitraghar.modules.payments.dto.request;

import com.chalchitraghar.modules.payments.enums.RefundManualReviewResolution;
import jakarta.validation.constraints.*;

public record AdminResolveRefundManualReviewRequest(
        @NotNull RefundManualReviewResolution resolution,
        @Size(max = 255) String externalReference,
        @Size(max = 500) String note) {}
