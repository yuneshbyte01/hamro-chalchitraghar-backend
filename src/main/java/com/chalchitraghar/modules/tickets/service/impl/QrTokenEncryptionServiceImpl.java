package com.chalchitraghar.modules.tickets.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import com.chalchitraghar.modules.tickets.config.TicketQrProperties;
import com.chalchitraghar.modules.tickets.service.QrTokenEncryptionService;

@Service
public class QrTokenEncryptionServiceImpl implements QrTokenEncryptionService {
    private static final int IV_BYTES=12, TAG_BITS=128;
    private final TicketQrProperties properties;
    private final SecureRandom random=new SecureRandom();
    public QrTokenEncryptionServiceImpl(TicketQrProperties properties){this.properties=properties;}
    public String encrypt(String rawToken){
        if(rawToken==null||rawToken.isBlank())throw new IllegalArgumentException("QR token is required");
        try{
            byte[] iv=new byte[IV_BYTES];random.nextBytes(iv);
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,new SecretKeySpec(properties.decodedKey(),"AES"),new GCMParameterSpec(TAG_BITS,iv));
            cipher.updateAAD(aad());
            byte[] ciphertext=cipher.doFinal(rawToken.getBytes(StandardCharsets.UTF_8));
            var enc=Base64.getUrlEncoder().withoutPadding();
            return "1."+properties.keyId()+"."+enc.encodeToString(iv)+"."+enc.encodeToString(ciphertext);
        }catch(Exception ex){throw new IllegalStateException("Unable to encrypt QR token",ex);}
    }
    public String decrypt(String encryptedToken){
        try{
            String[] parts=encryptedToken.split("\\.",-1);
            if(parts.length!=4||!"1".equals(parts[0])||!properties.keyId().equals(parts[1]))throw new IllegalArgumentException();
            var dec=Base64.getUrlDecoder(); byte[] iv=dec.decode(parts[2]); byte[] ciphertext=dec.decode(parts[3]);
            if(iv.length!=IV_BYTES)throw new IllegalArgumentException();
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,new SecretKeySpec(properties.decodedKey(),"AES"),new GCMParameterSpec(TAG_BITS,iv));
            cipher.updateAAD(aad());
            return new String(cipher.doFinal(ciphertext),StandardCharsets.UTF_8);
        }catch(Exception ex){throw new IllegalStateException("Unable to decrypt QR token",ex);}
    }
    private byte[] aad(){return ("ticket-qr|1|"+properties.keyId()).getBytes(StandardCharsets.UTF_8);}
}
