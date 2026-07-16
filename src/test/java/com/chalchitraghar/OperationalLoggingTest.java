package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import com.chalchitraghar.shared.observability.LogSanitizer;
import com.chalchitraghar.shared.observability.ProviderFailureCategory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.logging.logback.StructuredLogEncoder;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

class OperationalLoggingTest {
    @Test
    void externalTextCannotForgeLogLinesAndIsBounded() {
        String sanitized = LogSanitizer.safe("provider\r\nERROR forged\u0000" + "x".repeat(500));

        assertThat(sanitized).doesNotContain("\r", "\n", "\u0000").hasSize(300);
    }

    @Test
    void providerFailuresUseBoundedCategories() {
        assertThat(ProviderFailureCategory.classify(new SocketTimeoutException()))
                .isEqualTo(ProviderFailureCategory.TIMEOUT);
        assertThat(ProviderFailureCategory.classify(new IllegalStateException("secret response")))
                .isEqualTo(ProviderFailureCategory.UNKNOWN);
    }

    @Test
    void nativeProductionEncoderProducesOneValidJsonEventWithMdc() throws Exception {
        LoggerContext context = new LoggerContext();
        context.putObject(Environment.class.getName(), new MockEnvironment());
        StructuredLogEncoder encoder = new StructuredLogEncoder();
        encoder.setContext(context);
        encoder.setFormat("ecs");
        encoder.start();

        LoggingEvent event = new LoggingEvent();
        event.setLoggerContext(context);
        event.setLoggerName("com.chalchitraghar.Test");
        event.setThreadName("test-thread");
        event.setLevel(Level.INFO);
        event.setMessage("structured event");
        event.setMDCPropertyMap(Map.of("requestId", "request-1", "correlationId", "correlation-1"));

        String encoded = new String(encoder.encode(event), StandardCharsets.UTF_8);
        JsonNode json = new ObjectMapper().readTree(encoded);

        assertThat(encoded.lines()).hasSize(1);
        assertThat(json.at("/log/level").asText()).isEqualTo("INFO");
        assertThat(json.path("message").asText()).isEqualTo("structured event");
        assertThat(json.path("requestId").asText()).isEqualTo("request-1");
        assertThat(json.path("correlationId").asText()).isEqualTo("correlation-1");
        encoder.stop();
        context.stop();
    }
}
