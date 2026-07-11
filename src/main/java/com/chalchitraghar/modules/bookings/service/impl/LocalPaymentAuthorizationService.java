package com.chalchitraghar.modules.bookings.service.impl;

import org.springframework.stereotype.Service;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.service.PaymentAuthorizationService;

/** Backward-compatible local authorization until a payment provider is integrated. */
@Service
public class LocalPaymentAuthorizationService implements PaymentAuthorizationService {
    @Override
    public boolean authorize(Booking booking) {
        return true;
    }
}
