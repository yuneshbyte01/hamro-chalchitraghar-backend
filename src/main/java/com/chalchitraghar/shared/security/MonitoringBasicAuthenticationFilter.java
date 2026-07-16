package com.chalchitraghar.shared.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Dedicated, environment-injected scrape credential; never authenticates application APIs. */
@Component
public class MonitoringBasicAuthenticationFilter extends OncePerRequestFilter {
    private final String username;
    private final String password;

    public MonitoringBasicAuthenticationFilter(
            @Value("${management.prometheus.security.username:}") String username,
            @Value("${management.prometheus.security.password:}") String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !"/actuator/prometheus".equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (!username.isBlank()
                && !password.isBlank()
                && header != null
                && header.startsWith("Basic ")) {
            authenticate(header.substring(6));
        }
        filterChain.doFilter(request, response);
    }

    private void authenticate(String encoded) {
        try {
            String decoded =
                    new String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8);
            int separator = decoded.indexOf(':');
            if (separator > 0
                    && constantTimeEquals(username, decoded.substring(0, separator))
                    && constantTimeEquals(password, decoded.substring(separator + 1))) {
                SecurityContextHolder.getContext()
                        .setAuthentication(
                                new UsernamePasswordAuthenticationToken(
                                        "prometheus",
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_MONITORING"))));
            }
        } catch (IllegalArgumentException ignored) {
            // Malformed Basic values remain unauthenticated and use the normal 401 response.
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
