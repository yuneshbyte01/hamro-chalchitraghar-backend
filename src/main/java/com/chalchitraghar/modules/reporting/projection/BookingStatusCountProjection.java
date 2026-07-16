package com.chalchitraghar.modules.reporting.projection;

import com.chalchitraghar.modules.bookings.enums.BookingStatus;

public interface BookingStatusCountProjection {
    BookingStatus getStatus();

    long getCount();
}
