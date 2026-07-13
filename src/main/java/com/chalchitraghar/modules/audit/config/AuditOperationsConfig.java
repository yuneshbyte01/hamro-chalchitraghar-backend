package com.chalchitraghar.modules.audit.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AuditOperationsProperties.class)
public class AuditOperationsConfig {}
