package com.chalchitraghar.shared.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for Spring MVC.
 * Note: This configuration may be redundant if CORS is already configured in SecurityConfig.
 */
@Configuration
public class CORSConfig implements WebMvcConfigurer {

    /**
     * Configures CORS mappings to allow cross-origin requests from the frontend.
     *
     * @param registry the CORS registry
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry
            .addMapping("/**")
            .allowedOrigins("http://localhost:4200")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true);
    }
}