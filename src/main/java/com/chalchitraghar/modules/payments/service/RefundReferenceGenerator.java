package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.repository.RefundRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RefundReferenceGenerator {
    private static final int MAX_ATTEMPTS = 10;
    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private final RefundRepository repository;
    private final Clock clock;

    public String generate() {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            String suffix =
                    UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String reference = "RFD-" + LocalDate.now(clock).format(DATE) + "-" + suffix;
            if (!repository.existsByRefundReference(reference)) return reference;
        }
        throw new IllegalStateException("Unable to generate a unique refund reference");
    }
}
