package com.chalchitraghar.modules.bookings.service;

import com.chalchitraghar.modules.bookings.entity.Booking;

/** Seam for future external payment verification before confirmation. */
public interface PaymentAuthorizationService {
    boolean authorize(Booking booking);
}
