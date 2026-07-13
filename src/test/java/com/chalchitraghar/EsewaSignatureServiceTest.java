package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import com.chalchitraghar.modules.payments.config.EsewaProperties;
import com.chalchitraghar.modules.payments.esewa.EsewaSignatureService;
import java.math.BigDecimal;
import java.net.URI;
import org.junit.jupiter.api.Test;

class EsewaSignatureServiceTest {
    @Test
    void matchesOfficialSandboxSignatureExample() {
        var p =
                new EsewaProperties(
                        true,
                        "SANDBOX",
                        "EPAYTEST",
                        "8gBm/:&EnhH.1/q",
                        URI.create("https://example.com"),
                        URI.create("https://example.com"),
                        URI.create("https://example.com"),
                        URI.create("https://example.com"),
                        5000,
                        10000);
        var s = new EsewaSignatureService(p);
        assertThat(s.requestSignature(new BigDecimal("110.00"), "241028"))
                .isEqualTo("i94zsd3oXF6ZsSr/kGqT4sSzYQzjj1W/waxjWyRwaME=");
        assertThat(s.amount(new BigDecimal("100.00"))).isEqualTo("100");
    }
}
