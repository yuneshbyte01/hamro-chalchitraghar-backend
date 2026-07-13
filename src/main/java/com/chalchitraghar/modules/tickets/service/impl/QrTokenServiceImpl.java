package com.chalchitraghar.modules.tickets.service.impl;

import com.chalchitraghar.modules.tickets.config.TicketQrProperties;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.service.QrTokenEncryptionService;
import com.chalchitraghar.modules.tickets.service.QrTokenService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class QrTokenServiceImpl implements QrTokenService {
    private final SecureRandom random = new SecureRandom();
    private final QrTokenEncryptionService encryption;
    private final TicketQrProperties properties;

    public QrTokenServiceImpl(QrTokenEncryptionService encryption, TicketQrProperties properties) {
        this.encryption = encryption;
        this.properties = properties;
    }

    public String generateToken() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashToken(String raw) {
        if (raw == null) throw new IllegalArgumentException("QR token is required");
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash QR token", e);
        }
    }

    public PreparedQrToken prepareToken() {
        String raw = generateToken();
        return new PreparedQrToken(
                encryption.encrypt(raw),
                hashToken(raw),
                properties.tokenVersion(),
                properties.keyId());
    }

    public String decryptToken(Ticket ticket) {
        return encryption.decrypt(ticket.getQrTokenEncrypted());
    }

    public boolean matches(String raw, String stored) {
        if (stored == null) return false;
        return MessageDigest.isEqual(
                hashToken(raw).getBytes(StandardCharsets.US_ASCII),
                stored.getBytes(StandardCharsets.US_ASCII));
    }
}
