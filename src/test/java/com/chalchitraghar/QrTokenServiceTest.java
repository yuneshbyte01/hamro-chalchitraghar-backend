package com.chalchitraghar;

import static org.assertj.core.api.Assertions.*;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import com.chalchitraghar.modules.tickets.config.TicketQrProperties;
import com.chalchitraghar.modules.tickets.service.impl.QrTokenEncryptionServiceImpl;
import com.chalchitraghar.modules.tickets.service.impl.QrTokenServiceImpl;

class QrTokenServiceTest {
    private TicketQrProperties properties() {
        return new TicketQrProperties(Base64.getEncoder().encodeToString("0123456789ABCDEF0123456789ABCDEF".getBytes()),
                "qr-test-v1",1,400,2,60,30);
    }

    @Test void tokensHaveAtLeast256BitsAndAreUniqueAndHashMatches() {
        var encryption=new QrTokenEncryptionServiceImpl(properties()); var service=new QrTokenServiceImpl(encryption,properties());
        String first=service.generateToken(),second=service.generateToken();
        assertThat(first).isNotEqualTo(second);
        assertThat(Base64.getUrlDecoder().decode(first)).hasSize(32);
        assertThat(service.matches(first,service.hashToken(first))).isTrue();
        assertThat(service.matches(second,service.hashToken(first))).isFalse();
    }

    @Test void authenticatedEncryptionRoundTripsUsesRandomIvAndRejectsTamperingAndWrongKey() {
        var service=new QrTokenEncryptionServiceImpl(properties()); String raw="opaque-token";
        String first=service.encrypt(raw),second=service.encrypt(raw);
        assertThat(first).isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo(raw);
        String[] parts=first.split("\\."); byte[] changed=Base64.getUrlDecoder().decode(parts[3]); changed[0]^=1;
        String tampered=parts[0]+"."+parts[1]+"."+parts[2]+"."+Base64.getUrlEncoder().withoutPadding().encodeToString(changed);
        assertThatThrownBy(()->service.decrypt(tampered)).isInstanceOf(IllegalStateException.class);
        var wrong=new TicketQrProperties(Base64.getEncoder().encodeToString("FEDCBA9876543210FEDCBA9876543210".getBytes()),"qr-test-v1",1,400,2,60,30);
        assertThatThrownBy(()->new QrTokenEncryptionServiceImpl(wrong).decrypt(first)).isInstanceOf(IllegalStateException.class);
    }

    @Test void invalidKeyConfigurationFailsFast() {
        assertThatThrownBy(()->new TicketQrProperties("c2hvcnQ=","key",1,400,2,60,30))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("32 bytes");
    }
}
