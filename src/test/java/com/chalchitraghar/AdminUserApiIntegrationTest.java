package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;

class AdminUserApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getAdminUsersReturnsSummaryForAdmin() throws Exception {
        String adminToken = tokenFor("admin-list@example.com", Role.ADMIN);
        User customer = saveUser("customer-list@example.com", Role.CUSTOMER);
        customer.setEnabled(false);
        customer.setLocked(true);
        userRepository.save(customer);

        MvcResult result = mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Users fetched successfully"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[*].email", hasItems(
                        "admin-list@example.com",
                        "customer-list@example.com")))
                .andExpect(jsonPath("$.data[*].enabled").isArray())
                .andExpect(jsonPath("$.data[*].locked").isArray())
                .andExpect(jsonPath("$.data[0].password").doesNotExist())
                .andExpect(jsonPath("$.data[0].googleId").doesNotExist())
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("\"password\":");
        assertThat(body).doesNotContain("\"googleId\":");
        assertThat(body).doesNotContain("\"otp");
    }

    @Test
    void getAdminUsersForbidsStaff() throws Exception {
        String staffToken = tokenFor("staff-list-denied@example.com", Role.STAFF);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getAdminUsersForbidsCustomer() throws Exception {
        String customerToken = tokenFor("customer-list-denied@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getAdminUsersRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getAdminUserByIdReturnsDetailForAdmin() throws Exception {
        String adminToken = tokenFor("admin-detail@example.com", Role.ADMIN);
        User user = saveUser("detail-user@example.com", Role.CUSTOMER);
        user.setEmailVerified(true);
        user.setEnabled(false);
        user.setLocked(true);
        user.setFailedLoginAttempts(3);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        user.setLastLoginAt(LocalDateTime.now().minusHours(1));
        user.setPasswordChangedAt(LocalDateTime.now().minusDays(1));
        user = userRepository.save(user);

        MvcResult result = mockMvc.perform(get("/api/admin/users/{id}", user.getId())
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User fetched successfully"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.email").value("detail-user@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.authProvider").value("LOCAL"))
                .andExpect(jsonPath("$.data.emailVerified").value(true))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.locked").value(true))
                .andExpect(jsonPath("$.data.failedLoginAttempts").value(3))
                .andExpect(jsonPath("$.data.lastLoginAt").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.googleId").doesNotExist())
                .andExpect(jsonPath("$.data.otpHash").doesNotExist())
                .andExpect(jsonPath("$.errors").isArray())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("\"password\":");
        assertThat(body).doesNotContain("\"googleId\":");
        assertThat(body).doesNotContain("\"otp");
    }

    @Test
    void getAdminUserByIdReturnsNotFoundForUnknownUser() throws Exception {
        String adminToken = tokenFor("admin-missing@example.com", Role.ADMIN);

        mockMvc.perform(get("/api/admin/users/99999")
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User not found with id: 99999"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getAdminUserByIdForbidsCustomer() throws Exception {
        String customerToken = tokenFor("customer-detail-denied@example.com", Role.CUSTOMER);
        User user = saveUser("detail-denied-customer-target@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/admin/users/{id}", user.getId())
                        .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getAdminUserByIdForbidsStaff() throws Exception {
        String staffToken = tokenFor("staff-detail-denied@example.com", Role.STAFF);
        User user = saveUser("detail-denied-staff-target@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/admin/users/{id}", user.getId())
                        .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }
}
