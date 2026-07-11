package com.chalchitraghar.modules.payments.dto.request;
import jakarta.validation.constraints.*;
public record EsewaVerificationRequest(@NotBlank @Size(max=16384) String data) {}
