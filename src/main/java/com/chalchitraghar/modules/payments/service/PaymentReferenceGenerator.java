package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentReferenceGenerator {
    private static final char[] CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();
    private final PaymentRepository paymentRepository;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder suffix = new StringBuilder(8);
            for (int i = 0; i < 8; i++) suffix.append(CHARS[random.nextInt(CHARS.length)]);
            String reference =
                    "PAY-"
                            + LocalDate.now(clock).format(DateTimeFormatter.BASIC_ISO_DATE)
                            + "-"
                            + suffix;
            if (!paymentRepository.existsByPaymentReference(reference)) return reference;
        }
        throw new IllegalStateException("Could not generate a unique payment reference");
    }
}
