package com.chalchitraghar.modules.payments.dto.request;
import jakarta.validation.constraints.NotNull;
public record LocalPaymentProcessRequest(@NotNull LocalPaymentResult result) { public enum LocalPaymentResult { SUCCESS, FAILED } }
