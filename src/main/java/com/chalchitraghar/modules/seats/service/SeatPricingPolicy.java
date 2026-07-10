package com.chalchitraghar.modules.seats.service;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.seats.enums.SeatType;

/** Authoritative pricing policy for generated show seats. */
@Component
public class SeatPricingPolicy {

    public double priceFor(SeatType seatType) {
        if (seatType == null) {
            throw new IllegalArgumentException("Seat type is required for pricing");
        }
        return switch (seatType) {
            case PREMIUM -> 750.0;
            case PLATINUM -> 500.0;
        };
    }
}
