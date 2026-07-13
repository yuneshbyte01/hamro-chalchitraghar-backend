package com.chalchitraghar.modules.seats.service;

import com.chalchitraghar.modules.seats.enums.SeatType;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

/** Authoritative pricing policy for generated show seats. */
@Component
public class SeatPricingPolicy {

    public BigDecimal priceFor(SeatType seatType) {
        if (seatType == null) {
            throw new IllegalArgumentException("Seat type is required for pricing");
        }
        return switch (seatType) {
            case PREMIUM -> new BigDecimal("750.00");
            case PLATINUM -> new BigDecimal("500.00");
        };
    }
}
