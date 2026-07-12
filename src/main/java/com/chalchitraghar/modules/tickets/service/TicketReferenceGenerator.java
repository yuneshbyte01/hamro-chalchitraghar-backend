package com.chalchitraghar.modules.tickets.service;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TicketReferenceGenerator {
    private static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final SecureRandom random = new SecureRandom();
    private final Clock clock;

    public String generate() {
        StringBuilder suffix = new StringBuilder(8);
        for (int i = 0; i < 8; i++) suffix.append(CHARS[random.nextInt(CHARS.length)]);
        return "TKT-" + LocalDate.now(clock).format(DATE) + "-" + suffix;
    }
}
