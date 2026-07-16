package com.chalchitraghar.modules.reporting.dto.response;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;

public record BookingStatusCountResponse(BookingStatus status, long count) {}
