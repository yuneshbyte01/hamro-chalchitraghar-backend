package com.chalchitraghar.modules.tickets.service;

import com.chalchitraghar.modules.tickets.entity.Ticket;

public interface QrTokenService {
    String generateToken();

    String hashToken(String rawToken);

    PreparedQrToken prepareToken();

    String decryptToken(Ticket ticket);

    boolean matches(String rawToken, String storedHash);

    record PreparedQrToken(String encryptedToken, String tokenHash, int version, String keyId) {}
}
