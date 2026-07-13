package com.chalchitraghar.shared.security;

import com.chalchitraghar.modules.audit.service.AuditSecurityRecorder;
import com.chalchitraghar.modules.users.repository.UserRepository;
import com.chalchitraghar.shared.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter that intercepts HTTP requests to extract and validate JWT tokens. Sets up Spring Security
 * authentication context for authenticated users.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final AuditSecurityRecorder auditSecurity;
    private final Clock clock;

    /**
     * Processes each request to extract JWT token from Authorization header, validates it, and sets
     * up authentication context if valid.
     *
     * @param request the HTTP request
     * @param response the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String email = null;
        String token = null;

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);

            try {
                email = jwtUtil.extractUsername(token);
                if (!jwtUtil.validateToken(token)) {
                    email = null;
                }
            } catch (ExpiredJwtException e) {
                logger.warn("JWT rejected reason=TOKEN_EXPIRED");
                auditSecurity.invalidJwt("TOKEN_EXPIRED");
                writeUnauthorizedResponse(response, "Token expired");
                return;
            } catch (Exception e) {
                logger.warn("JWT rejected reason=TOKEN_INVALID");
                auditSecurity.invalidJwt("TOKEN_INVALID");
                writeUnauthorizedResponse(response, "Invalid token");
                return;
            }
        }

        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var user = userRepository.findByEmail(email).orElse(null);

            if (user != null) {
                if (!user.isEnabled()) {
                    writeUnauthorizedResponse(response, "Account is disabled");
                    return;
                }
                if (isLocked(user)) {
                    writeUnauthorizedResponse(
                            response, "Account is temporarily locked. Please try again later.");
                    return;
                }
                if (jwtUtil.wasIssuedBeforePasswordChanged(token, user)) {
                    writeUnauthorizedResponse(
                            response, "Token is no longer valid after password change");
                    return;
                }

                String role = jwtUtil.extractRole(token);
                List<SimpleGrantedAuthority> authorities =
                        role != null
                                ? Collections.singletonList(
                                        new SimpleGrantedAuthority("ROLE_" + role))
                                : Collections.emptyList();

                var authToken = new UsernamePasswordAuthenticationToken(user, null, authorities);

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                MDC.put("actorUserId", String.valueOf(user.getId()));
                MDC.put("actorRole", user.getRole().name());
            }
        }

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(HttpServletResponse response, String message)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.error(message)));
    }

    private boolean isLocked(com.chalchitraghar.modules.users.entity.User user) {
        return user.isLocked()
                && (user.getLockedUntil() == null
                        || user.getLockedUntil().isAfter(LocalDateTime.now(clock)));
    }
}
