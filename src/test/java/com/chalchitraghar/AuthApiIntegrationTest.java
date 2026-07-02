package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;

class AuthApiIntegrationTest extends AbstractIntegrationTest {

    private static final String STRONG_PASSWORD = "Password123!";

    @Test
    void registerCustomerSuccessfullyWithStrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Sita",
                                "email", "sita@example.com",
                                "password", STRONG_PASSWORD))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User registered successfully"))
                .andExpect(jsonPath("$.data.email").value("sita@example.com"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void registerFailsWithoutUppercase() throws Exception {
        expectWeakPasswordRegistrationFails("password123!");
    }

    @Test
    void registerFailsWithoutLowercase() throws Exception {
        expectWeakPasswordRegistrationFails("PASSWORD123!");
    }

    @Test
    void registerFailsWithoutDigit() throws Exception {
        expectWeakPasswordRegistrationFails("Password!");
    }

    @Test
    void registerFailsWithoutSpecialCharacter() throws Exception {
        expectWeakPasswordRegistrationFails("Password123");
    }

    @Test
    void loginSuccessfully() throws Exception {
        saveUser("login@example.com", Role.CUSTOMER);

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
        saveUser("duplicate@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Duplicate",
                                "email", "duplicate@example.com",
                                "password", STRONG_PASSWORD))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void invalidLoginReturnsUnauthorizedErrorResponse() throws Exception {
        saveUser("wrong-password@example.com", Role.CUSTOMER);

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
                                "password", STRONG_PASSWORD))))
                .andExpect(status().isCreated());

        User user = userRepository.findByEmail("hashed@example.com").orElseThrow();
        assertThat(user.getPassword()).isNotEqualTo(STRONG_PASSWORD);
        assertThat(passwordEncoder.matches(STRONG_PASSWORD, user.getPassword())).isTrue();
    }

    @Test
    void refreshTokenSuccessfully() throws Exception {
        String token = tokenFor("refresh@example.com", Role.CUSTOMER);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(json(Map.of("token", token))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Token refreshed successfully"))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.email").value("refresh@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void invalidRefreshTokenReturnsUnauthorizedErrorResponse() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType("application/json")
                        .content(json(Map.of("token", "invalid-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid or expired token"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerProfileReturnsCurrentUserForValidCustomerToken() throws Exception {
        String token = tokenFor("profile@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/customer/profile")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile fetched successfully"))
                .andExpect(jsonPath("$.data.name").value("CUSTOMER User"))
                .andExpect(jsonPath("$.data.email").value("profile@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerProfileUpdateChangesName() throws Exception {
        String token = tokenFor("profile-update@example.com", Role.CUSTOMER);

        mockMvc.perform(put("/api/customer/profile")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of("name", "Updated Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Profile updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"))
                .andExpect(jsonPath("$.data.email").value("profile-update@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.errors").isArray());

        User user = userRepository.findByEmail("profile-update@example.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("Updated Name");
    }

    @Test
    void customerProfileUpdateRejectsBlankName() throws Exception {
        String token = tokenFor("blank-name@example.com", Role.CUSTOMER);

        mockMvc.perform(put("/api/customer/profile")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of("name", " "))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]", containsString("name:")));
    }

    @Test
    void customerProfileUpdateDoesNotAllowEmailOrRoleUpdate() throws Exception {
        String token = tokenFor("protected-profile@example.com", Role.CUSTOMER);

        mockMvc.perform(put("/api/customer/profile")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Protected Name",
                                "email", "changed@example.com",
                                "role", "ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Protected Name"))
                .andExpect(jsonPath("$.data.email").value("protected-profile@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"));

        User user = userRepository.findByEmail("protected-profile@example.com").orElseThrow();
        assertThat(user.getName()).isEqualTo("Protected Name");
        assertThat(user.getEmail()).isEqualTo("protected-profile@example.com");
        assertThat(user.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(userRepository.findByEmail("changed@example.com")).isEmpty();
    }

    @Test
    void customerPasswordChangeSuccessfullyChangesPassword() throws Exception {
        saveCustomerWithPassword("change-password@example.com", "OldPass@123");
        String token = loginTokenWithPassword("change-password@example.com", "OldPass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "OldPass@123",
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Password changed successfully"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "change-password@example.com",
                                "password", "OldPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "email", "change-password@example.com",
                                "password", "NewStrongPass@123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isNotEmpty());
    }

    @Test
    void customerPasswordChangeFailsWithWrongCurrentPassword() throws Exception {
        saveCustomerWithPassword("wrong-current@example.com", "OldPass@123");
        String token = loginTokenWithPassword("wrong-current@example.com", "OldPass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "WrongPass@123",
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Current password is incorrect"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void customerPasswordChangeFailsWithWeakNewPassword() throws Exception {
        saveCustomerWithPassword("weak-new@example.com", "OldPass@123");
        String token = loginTokenWithPassword("weak-new@example.com", "OldPass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "OldPass@123",
                                "newPassword", "weakpassword"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]", containsString("newPassword:")));
    }

    @Test
    void customerPasswordChangeFailsWhenNewPasswordMatchesCurrentPassword() throws Exception {
        saveCustomerWithPassword("same-password@example.com", "SamePass@123");
        String token = loginTokenWithPassword("same-password@example.com", "SamePass@123");

        mockMvc.perform(put("/api/customer/profile/password")
                        .header("Authorization", bearer(token))
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "SamePass@123",
                                "newPassword", "SamePass@123"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("New password must be different from current password"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void unauthenticatedProfileUpdateReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/customer/profile")
                        .contentType("application/json")
                        .content(json(Map.of("name", "Updated Name"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void unauthenticatedPasswordChangeReturnsUnauthorized() throws Exception {
        mockMvc.perform(put("/api/customer/profile/password")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "currentPassword", "OldPass@123",
                                "newPassword", "NewStrongPass@123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    private void expectWeakPasswordRegistrationFails(String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(json(Map.of(
                                "name", "Weak Password",
                                "email", "weak-" + password.hashCode() + "@example.com",
                                "password", password))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0]", containsString("password:")));
    }

    private User saveCustomerWithPassword(String email, String password) {
        User user = User.builder()
                .name("CUSTOMER User")
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.CUSTOMER)
                .build();
        return userRepository.save(user);
    }

    private String loginTokenWithPassword(String email, String password) throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(json(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data")
                .path("token")
                .asText();
    }
}
