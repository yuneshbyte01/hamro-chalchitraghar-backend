package com.chalchitraghar.modules.reporting.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class ReportingWebConfig implements WebMvcConfigurer {
    private final ReportingMetricsInterceptor metrics;

    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(metrics).addPathPatterns("/api/admin/reports/**");
    }
}
