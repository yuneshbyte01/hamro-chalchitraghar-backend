package com.chalchitraghar.modules.tickets.service;
public interface QrTokenEncryptionService {
    String encrypt(String rawToken);
    String decrypt(String encryptedToken);
}
