package com.chalchitraghar.modules.tickets.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({TicketQrProperties.class, TicketOperationsProperties.class})
public class TicketQrConfig {}
