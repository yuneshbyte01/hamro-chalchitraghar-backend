package com.chalchitraghar.shared.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuditRequestContextProperties.class)
public class TimeConfig {

    @Bean
    Clock applicationClock(@Value("${app.time-zone:Asia/Kathmandu}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
