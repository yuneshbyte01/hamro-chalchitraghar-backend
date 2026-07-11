package com.chalchitraghar.modules.bookings.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import com.chalchitraghar.modules.bookings.repository.BookingRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BookingReferenceGenerator {
    private static final char[] ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private final BookingRepository bookingRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder suffix = new StringBuilder(8);
            for (int i = 0; i < 8; i++) suffix.append(ALPHANUMERIC[random.nextInt(ALPHANUMERIC.length)]);
            String reference = "HCG-" + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE) + "-" + suffix;
            if (!bookingRepository.existsByBookingReference(reference)) return reference;
        }
        throw new IllegalStateException("Could not generate a unique booking reference");
    }
}
