package com.chalchitraghar.modules.payments.dto.response;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.payments.enums.RefundStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerBookingCancellationRefundResponse(
        String bookingReference,
        BookingStatus bookingStatus,
        String refundReference,
        RefundStatus refundStatus,
        BigDecimal amount,
        String currency,
        LocalDateTime requestedAt,
        String message) {}
