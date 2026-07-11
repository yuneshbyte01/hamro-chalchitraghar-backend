package com.chalchitraghar.modules.payments.dto.request;
import com.chalchitraghar.modules.payments.enums.*;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
@Schema(description="Payment provider selection; amount, currency, customer and booking are server-authoritative")
public record PaymentInitiationRequest(@NotNull PaymentProvider provider, @NotNull PaymentMethod method) {}
