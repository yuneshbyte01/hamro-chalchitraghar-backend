package com.chalchitraghar.modules.payments.dto.response;

public record PaymentConsistencyIssue(
        String type, String paymentReference, String bookingReference, String description) {}
