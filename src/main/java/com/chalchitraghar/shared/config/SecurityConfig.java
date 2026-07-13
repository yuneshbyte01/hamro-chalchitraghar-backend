package com.chalchitraghar.shared.config;

import com.chalchitraghar.modules.audit.service.AuditSecurityRecorder;
import com.chalchitraghar.shared.audit.RequestAuditContextFilter;
import com.chalchitraghar.shared.response.ApiResponse;
import com.chalchitraghar.shared.security.JwtAuthenticationFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Security configuration for Spring Security. Access control is enforced here via requestMatchers;
 * controllers do not use @PreAuthorize. - Public: /api/auth/**, /api/public/** - Customer:
 * /api/customer/** - Staff: /api/staff/** - Admin: /api/admin/**
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;
    private final RequestAuditContextFilter requestAuditContextFilter;
    private final AuditSecurityRecorder auditSecurity;

    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    /**
     * Provides BCrypt password encoder bean for password hashing.
     *
     * @return BCryptPasswordEncoder instance
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configures CORS settings to allow requests from the frontend application.
     *
     * @return CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(parseAllowedOrigins());
        configuration.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> parseAllowedOrigins() {
        return Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    /**
     * Configures the security filter chain with JWT authentication and role-based authorization.
     *
     * @param http HttpSecurity instance
     * @return configured SecurityFilterChain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/swagger-ui.html",
                                                "/swagger-ui/**",
                                                "/v3/api-docs",
                                                "/v3/api-docs/**")
                                        .permitAll()
                                        .requestMatchers("/api/auth/**")
                                        .permitAll()
                                        .requestMatchers("/api/public/**")
                                        .permitAll()
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.POST,
                                                "/api/payments/esewa/verify")
                                        .permitAll()
                                        .requestMatchers("/api/customer/**")
                                        .hasAnyRole("CUSTOMER", "STAFF", "ADMIN")
                                        .requestMatchers("/api/staff/**")
                                        .hasAnyRole("STAFF", "ADMIN")
                                        .requestMatchers("/api/admin/**")
                                        .hasRole("ADMIN")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        exception ->
                                exception
                                        .authenticationEntryPoint(
                                                (request, response, authException) -> {
                                                    response.setStatus(401);
                                                    response.setContentType(
                                                            MediaType.APPLICATION_JSON_VALUE);
                                                    response.getWriter()
                                                            .write(
                                                                    objectMapper.writeValueAsString(
                                                                            ApiResponse.error(
                                                                                    "Authentication required")));
                                                })
                                        .accessDeniedHandler(
                                                (request, response, accessDeniedException) -> {
                                                    auditSecurity.accessDenied();
                                                    response.setStatus(403);
                                                    response.setContentType(
                                                            MediaType.APPLICATION_JSON_VALUE);
                                                    response.getWriter()
                                                            .write(
                                                                    objectMapper.writeValueAsString(
                                                                            ApiResponse.error(
                                                                                    "Access denied")));
                                                }))
                .addFilterBefore(
                        jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(requestAuditContextFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}
