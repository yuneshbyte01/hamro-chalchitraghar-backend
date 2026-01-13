package com.chalchitraghar.security;

import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.chalchitraghar.model.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {

    // Use a secure, random secret key (at least 256 bits for HS256)
    private String secret = "4Qnni8zBXDBnVf9hOQpF5n1t8Oe9Lw1qEqnbiLdR5m4VxXXlYX09c18ZHq4JihAs";

    private long EXPIRATION_MS = 3600000; // 1h

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Generate a JWT token for the given user, embedding email and role
    public String generateToken(User user) {
        return Jwts.builder()
                .setSubject(user.getEmail())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .claim("role", user.getRole().name())
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)    
                .compact();
    }

    // Extract the username/email (subject) from a JWT token
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    // Extract role from the token
    public String extractRole(String token) {
        Object role = extractAllClaims(token).get("role");
        return (role == null) ? null : role.toString();
    }

    // Validate the integrity and expiration of a JWT token
    public boolean validateToken(String token) {
        try {
            extractAllClaims(token); // will throw if invalid or expired
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Helper: extract all claims
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
