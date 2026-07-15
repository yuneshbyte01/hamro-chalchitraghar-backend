package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.shows.enums.ShowStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminRefundDetailResponse(
        String refundReference,
        String bookingReference,
        String paymentReference,
        Long customerId,
        String customerName,
        String customerEmail,
        BigDecimal amount,
        String currency,
        RefundStatus status,
        RefundReason reason,
        RefundType type,
        RefundMethod method,
        PaymentProvider paymentProvider,
        PaymentStatus paymentStatus,
        BookingStatus bookingStatus,
        ShowStatus showStatus,
        TicketStatusSummary ticketStatusSummary,
        String requestedBy,
        String approvedBy,
        String rejectedBy,
        LocalDateTime requestedAt,
        LocalDateTime approvedAt,
        LocalDateTime rejectedAt,
        String providerRefundReference,
        String sanitizedFailureReason,
        String rejectionReasonCode,
        String rejectionNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {}
