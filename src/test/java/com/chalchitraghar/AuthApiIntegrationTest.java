package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chalchitraghar.modules.users.entity.User;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void registerCustomerSuccessfully() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Sita",
                                "email", "sita@example.com",
                                "password", "password123"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.email").value("sita@example.com"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void loginSuccessfully() throws Exception {
        saveUser("login@example.com", com.chalchitraghar.modules.users.enums.Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("email", "login@example.com", "password", "password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Login successful"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void duplicateEmailRegistrationReturnsError() throws Exception {
        saveUser("duplicate@example.com", com.chalchitraghar.modules.users.enums.Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Duplicate",
                                "email", "duplicate@example.com",
                                "password", "password123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void invalidLoginReturnsUnauthorizedErrorResponse() throws Exception {
        saveUser("wrong-password@example.com", com.chalchitraghar.modules.users.enums.Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("email", "wrong-password@example.com", "password", "wrongpass123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid credentials"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void passwordIsStoredHashed() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Hashed User",
                                "email", "hashed@example.com",
                                "password", "password123"))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("hashed@example.com").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", user.getPassword())).isTrue();
    }
}
