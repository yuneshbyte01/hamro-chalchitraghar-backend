package com.chalchitraghar;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.Test;

import com.chalchitraghar.modules.users.enums.Role;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

class AuthorizationAndErrorApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void publicEndpointsWorkWithoutToken() throws Exception {
        mockMvc.perform(get("/api/public/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Health check successful"))
                .andExpect(jsonPath("$.data.status").value("UP"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/customer/profile"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void adminEndpointsRequireAdminRole() throws Exception {
        String customerToken = tokenFor("not-admin@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/admin/movies")
                        .header("Authorization", bearer(customerToken))
                        .contentType("application/json")
                        .content(json(movieRequest("Forbidden Movie", com.chalchitraghar.modules.movies.enums.MovieStatus.NOW_SHOWING))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void staffEndpointFollowsSecurityRules() throws Exception {
        String customerToken = tokenFor("staff-denied@example.com", Role.CUSTOMER);
        String staffToken = tokenFor("staff-allowed@example.com", Role.STAFF);

        mockMvc.perform(get("/api/staff/bookings/1")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(get("/api/staff/bookings/1")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Booking not found with id: 1"));
    }

    @Test
    void invalidJwtReturnsStandardApiResponse() throws Exception {
        mockMvc.perform(get("/api/customer/profile")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid token"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void expiredJwtReturnsStandardApiResponse() throws Exception {
        String expiredToken = expiredToken();

        mockMvc.perform(get("/api/customer/profile")
                        .header("Authorization", bearer(expiredToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Token expired"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void validationErrorUsesStandardApiResponse() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "",
                                "email", "not-an-email",
                                "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]", containsString(":")));
    }

    @Test
    void notFoundResponseUsesStandardApiResponse() throws Exception {
        mockMvc.perform(get("/api/public/halls/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Hall not found with id: 99999"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private String expiredToken() {
        SecretKey key = Keys.hmacShaKeyFor("1ba8d994184ee1bb8e98f6a323f45a5c".getBytes());
        return Jwts.builder()
                .setSubject("expired@example.com")
                .setIssuedAt(new java.util.Date(System.currentTimeMillis() - 120_000))
                .setExpiration(new java.util.Date(System.currentTimeMillis() - 60_000))
                .claim("role", "CUSTOMER")
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
