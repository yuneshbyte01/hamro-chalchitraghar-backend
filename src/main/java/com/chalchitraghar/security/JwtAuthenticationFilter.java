package com.chalchitraghar.security;

import com.chalchitraghar.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // Utility for JWT operations (token generation, validation, extraction)
    private final JwtUtil jwtUtil;

    // Repository for retrieving user details from the database
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        String username = null;
        String token = null;

        // Extract token from the "Authorization" header if present
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring(7);

            try {
                username = jwtUtil.extractUsername(token);
            } catch (ExpiredJwtException e) {
                logger.error("JWT expired: " + e.getMessage());
            } catch (Exception e) {
                logger.error("JWT error: " + e.getMessage());
            }
        }

        // Authenticate user if username is valid and context is not yet set
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            var user = userRepository.findByEmail(username).orElse(null);

            if (user != null && jwtUtil.validateToken(token)) {
                // Create an authentication token with user details and role
                var authToken = new UsernamePasswordAuthenticationToken(
                        user, null,
                        user.getRole() != null
                                ? java.util.List.of(() -> user.getRole().name())
                                : java.util.List.of()
                );

                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}
