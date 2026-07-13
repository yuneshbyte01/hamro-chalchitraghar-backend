package com.chalchitraghar.modules.tickets.config;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.tickets.qr")
public record TicketQrProperties(
        String encryptionKey,
        String keyId,
        int tokenVersion,
        int imageSize,
        int imageMargin,
        long entryWindowMinutes,
        long postShowGraceMinutes) {
    public TicketQrProperties {
        if (encryptionKey == null || encryptionKey.isBlank())
            throw new IllegalArgumentException("Ticket QR encryption key is required");
        byte[] decoded;
        try {
            decoded = Base64.getDecoder().decode(encryptionKey);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Ticket QR encryption key must be valid Base64", ex);
        }
        if (decoded.length != 32)
            throw new IllegalArgumentException(
                    "Ticket QR encryption key must decode to exactly 32 bytes");
        if (keyId == null || keyId.isBlank())
            throw new IllegalArgumentException("Ticket QR key ID is required");
        if (tokenVersion < 1)
            throw new IllegalArgumentException("Ticket QR token version must be positive");
        if (imageSize < 100 || imageSize > 2000)
            throw new IllegalArgumentException("Ticket QR image size must be between 100 and 2000");
        if (imageMargin < 0 || imageMargin > 20)
            throw new IllegalArgumentException("Ticket QR image margin must be between 0 and 20");
        if (entryWindowMinutes < 0 || postShowGraceMinutes < 0)
            throw new IllegalArgumentException("Ticket validation windows must not be negative");
    }

    public byte[] decodedKey() {
        return Base64.getDecoder().decode(encryptionKey);
    }
}
