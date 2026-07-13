package com.chalchitraghar.shared.security;

import com.chalchitraghar.modules.users.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Utility class for JWT token generation, validation, and claim extraction. */
@Component
public class JwtUtil {

    /**
     * Secret key for signing and verifying JWT tokens. Must be at least 256 bits for HS256
     * algorithm.
     */
    private final String secret;

    /** Token expiration time in milliseconds. */
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms}") long expirationMs) {
        this.secret = secret;
        this.expirationMs = expirationMs;
    }

    /**
     * Generates a signing key from the secret string.
     *
     * @return the secret key for JWT operations
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generates a JWT token for the given user with email and role claims.
     *
     * @param user the user for whom to generate the token
     * @return the generated JWT token
     */
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .claim("role", user.getRole().name())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extracts the email (subject) from a JWT token.
     *
     * @param token the JWT token
     * @return the email address from the token subject
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * Extracts the role claim from a JWT token.
     *
     * @param token the JWT token
     * @return the role as a string, or null if not present
     */
    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return (role == null) ? null : role.toString();
    }

    public Date extractIssuedAt(String token) {
        return extractAllClaims(token).getIssuedAt();
    }

    public boolean wasIssuedBeforePasswordChanged(String token, User user) {
        if (user.getPasswordChangedAt() == null) {
            return false;
        }
        Date issuedAt = extractIssuedAt(token);
        Date passwordChangedAt =
                Date.from(user.getPasswordChangedAt().atZone(ZoneId.systemDefault()).toInstant());
        return issuedAt.before(passwordChangedAt);
    }

    /**
     * Validates the integrity and expiration of a JWT token.
     *
     * @param token the JWT token to validate
     * @return true if the token is valid and not expired, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extracts all claims from a JWT token.
     *
     * @param token the JWT token
     * @return the claims contained in the token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
