package com.chalchitraghar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.AuthProvider;
import com.chalchitraghar.modules.users.enums.Role;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

class AdminUserApiIntegrationTest extends AbstractIntegrationTest {

    @Test
    void getAdminUsersReturnsDefaultPaginatedSummaryForAdmin() throws Exception {
        String adminToken = tokenFor("admin-list@example.com", Role.ADMIN);
        User customer = saveUser("customer-list@example.com", Role.CUSTOMER);
        customer.setEnabled(false);
        customer.setLocked(true);
        userRepository.save(customer);

        MvcResult result =
                mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(adminToken)))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.message").value("Users fetched successfully"))
                        .andExpect(jsonPath("$.data.content").isArray())
                        .andExpect(jsonPath("$.data.page").value(0))
                        .andExpect(jsonPath("$.data.size").value(20))
                        .andExpect(jsonPath("$.data.totalElements").value(2))
                        .andExpect(jsonPath("$.data.totalPages").value(1))
                        .andExpect(jsonPath("$.data.last").value(true))
                        .andExpect(
                                jsonPath(
                                        "$.data.content[*].email",
                                        hasItems(
                                                "admin-list@example.com",
                                                "customer-list@example.com")))
                        .andExpect(jsonPath("$.data.content[*].enabled").isArray())
                        .andExpect(jsonPath("$.data.content[*].locked").isArray())
                        .andExpect(jsonPath("$.data.content[0].password").doesNotExist())
                        .andExpect(jsonPath("$.data.content[0].googleId").doesNotExist())
                        .andExpect(jsonPath("$.errors").isArray())
                        .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain("\"password\":");
        assertThat(body).doesNotContain("\"googleId\":");
        assertThat(body).doesNotContain("\"otp");
    }

    @Test
    void getAdminUsersSupportsPageAndSizeParameters() throws Exception {
        String adminToken = tokenFor("admin-page@example.com", Role.ADMIN);
        saveUser("page-a@example.com", Role.CUSTOMER);
        saveUser("page-b@example.com", Role.CUSTOMER);
        saveUser("page-c@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("page", "1")
                                .param("size", "2")
                                .param("sortBy", "email")
                                .param("sortDir", "asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(4))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    @Test
    void getAdminUsersSortsByNameAscending() throws Exception {
        String adminToken = tokenFor("admin-sort-name@example.com", Role.ADMIN);
        saveNamedUser("zeta@example.com", "Zeta User", Role.CUSTOMER);
        saveNamedUser("alpha@example.com", "Alpha User", Role.CUSTOMER);

        MvcResult result =
                mockMvc.perform(
                                get("/api/admin/users")
                                        .header("Authorization", bearer(adminToken))
                                        .param("role", "CUSTOMER")
                                        .param("sortBy", "name")
                                        .param("sortDir", "asc"))
                        .andExpect(status().isOk())
                        .andReturn();

        var content =
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .path("data")
                        .path("content");
        assertThat(content.get(0).path("name").asText()).isEqualTo("Alpha User");
        assertThat(content.get(1).path("name").asText()).isEqualTo("Zeta User");
    }

    @Test
    void getAdminUsersSortsByCreatedAtDescending() throws Exception {
        String adminToken = tokenFor("admin-sort-created@example.com", Role.ADMIN);
        saveNamedUser("older@example.com", "Older User", Role.CUSTOMER);
        Thread.sleep(20);
        saveNamedUser("newer@example.com", "Newer User", Role.CUSTOMER);

        MvcResult result =
                mockMvc.perform(
                                get("/api/admin/users")
                                        .header("Authorization", bearer(adminToken))
                                        .param("role", "CUSTOMER")
                                        .param("sortBy", "createdAt")
                                        .param("sortDir", "desc"))
                        .andExpect(status().isOk())
                        .andReturn();

        var content =
                objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .path("data")
                        .path("content");
        assertThat(content.get(0).path("email").asText()).isEqualTo("newer@example.com");
        assertThat(content.get(1).path("email").asText()).isEqualTo("older@example.com");
    }

    @Test
    void getAdminUsersRejectsInvalidSortBy() throws Exception {
        String adminToken = tokenFor("admin-invalid-sort-by@example.com", Role.ADMIN);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("sortBy", "password"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(org.hamcrest.Matchers.startsWith("Invalid sortBy")))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getAdminUsersRejectsInvalidSortDir() throws Exception {
        String adminToken = tokenFor("admin-invalid-sort-dir@example.com", Role.ADMIN);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("sortDir", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message").value("Invalid sortDir. Allowed values: asc, desc"))
                .andExpect(jsonPath("$.data").value(nullValue()))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void getAdminUsersSearchesByName() throws Exception {
        String adminToken = tokenFor("admin-search-name@example.com", Role.ADMIN);
        saveNamedUser("ram-name@example.com", "Ram Bahadur", Role.CUSTOMER);
        saveNamedUser("sita-name@example.com", "Sita Devi", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("search", "ram")
                                .param("role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("ram-name@example.com"));
    }

    @Test
    void getAdminUsersSearchesByEmail() throws Exception {
        String adminToken = tokenFor("admin-search-email@example.com", Role.ADMIN);
        saveNamedUser("ram.email@example.com", "Email Match", Role.CUSTOMER);
        saveNamedUser("sita.email@example.com", "Other Match", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("search", "ram.email")
                                .param("role", "CUSTOMER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("ram.email@example.com"));
    }

    @Test
    void getAdminUsersFiltersByRole() throws Exception {
        String adminToken = tokenFor("admin-filter-role@example.com", Role.ADMIN);
        saveUser("role-customer@example.com", Role.CUSTOMER);
        saveUser("role-staff@example.com", Role.STAFF);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("role", "STAFF"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("role-staff@example.com"))
                .andExpect(jsonPath("$.data.content[0].role").value("STAFF"));
    }

    @Test
    void getAdminUsersFiltersByEnabled() throws Exception {
        String adminToken = tokenFor("admin-filter-enabled@example.com", Role.ADMIN);
        User disabled = saveUser("disabled-filter@example.com", Role.CUSTOMER);
        disabled.setEnabled(false);
        userRepository.save(disabled);
        saveUser("enabled-filter@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("role", "CUSTOMER")
                                .param("enabled", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("disabled-filter@example.com"))
                .andExpect(jsonPath("$.data.content[0].enabled").value(false));
    }

    @Test
    void getAdminUsersFiltersByLocked() throws Exception {
        String adminToken = tokenFor("admin-filter-locked@example.com", Role.ADMIN);
        User locked = saveUser("locked-filter@example.com", Role.CUSTOMER);
        locked.setLocked(true);
        userRepository.save(locked);
        saveUser("unlocked-filter@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("role", "CUSTOMER")
                                .param("locked", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("locked-filter@example.com"))
                .andExpect(jsonPath("$.data.content[0].locked").value(true));
    }

    @Test
    void getAdminUsersFiltersByAuthProvider() throws Exception {
        String adminToken = tokenFor("admin-filter-provider@example.com", Role.ADMIN);
        saveGoogleUser("google-filter@example.com");
        saveUser("local-filter@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("authProvider", "GOOGLE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("google-filter@example.com"));
    }

    @Test
    void getAdminUsersFiltersByEmailVerified() throws Exception {
        String adminToken = tokenFor("admin-filter-verified@example.com", Role.ADMIN);
        User verified = saveUser("verified-filter@example.com", Role.CUSTOMER);
        verified.setEmailVerified(true);
        userRepository.save(verified);
        saveUser("unverified-filter@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("role", "CUSTOMER")
                                .param("emailVerified", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(
                        jsonPath("$.data.content[0].email").value("verified-filter@example.com"));
    }

    @Test
    void getAdminUsersCombinesSearchAndFilter() throws Exception {
        String adminToken = tokenFor("admin-combined@example.com", Role.ADMIN);
        User matching = saveNamedUser("ram-enabled@example.com", "Ram Enabled", Role.CUSTOMER);
        matching.setEnabled(true);
        userRepository.save(matching);
        User disabled = saveNamedUser("ram-disabled@example.com", "Ram Disabled", Role.CUSTOMER);
        disabled.setEnabled(false);
        userRepository.save(disabled);
        saveNamedUser("sita-enabled@example.com", "Sita Enabled", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("search", "ram")
                                .param("role", "CUSTOMER")
                                .param("enabled", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("ram-enabled@example.com"));
    }

    @Test
    void getAdminUsersRejectsInvalidRole() throws Exception {
        String adminToken = tokenFor("admin-invalid-role@example.com", Role.ADMIN);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("role", "OWNER"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(org.hamcrest.Matchers.startsWith("Invalid role")));
    }

    @Test
    void getAdminUsersRejectsInvalidAuthProvider() throws Exception {
        String adminToken = tokenFor("admin-invalid-provider@example.com", Role.ADMIN);

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("authProvider", "PASSWORD"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(org.hamcrest.Matchers.startsWith("Invalid authProvider")));
    }

    @Test
    void getAdminUsersForbidsStaff() throws Exception {
        String staffToken = tokenFor("staff-list-denied@example.com", Role.STAFF);

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getAdminUsersForbidsCustomer() throws Exception {
        String customerToken = tokenFor("customer-list-denied@example.com", Role.CUSTOMER);

        mockMvc.perform(get("/api/admin/users").header("Authorization", bearer(customerToken)))
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

        MvcResult result =
                mockMvc.perform(
                                get("/api/admin/users/{id}", user.getId())
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

        mockMvc.perform(get("/api/admin/users/99999").header("Authorization", bearer(adminToken)))
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

        mockMvc.perform(
                        get("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void getAdminUserByIdForbidsStaff() throws Exception {
        String staffToken = tokenFor("staff-detail-denied@example.com", Role.STAFF);
        User user = saveUser("detail-denied-staff-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        get("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void adminCreatesCustomer() throws Exception {
        String adminToken = tokenFor("admin-create-customer@example.com", Role.ADMIN);

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(
                                        json(
                                                createUserRequest(
                                                        "created-customer@example.com",
                                                        Role.CUSTOMER))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User created successfully"))
                .andExpect(jsonPath("$.data.email").value("created-customer@example.com"))
                .andExpect(jsonPath("$.data.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.authProvider").value("LOCAL"))
                .andExpect(jsonPath("$.data.emailVerified").value(false))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.locked").value(false))
                .andExpect(jsonPath("$.data.failedLoginAttempts").value(0))
                .andExpect(jsonPath("$.data.lockedUntil").value(nullValue()))
                .andExpect(jsonPath("$.data.lastLoginAt").value(nullValue()))
                .andExpect(jsonPath("$.data.passwordChangedAt").exists())
                .andExpect(jsonPath("$.data.password").doesNotExist());

        User created = userRepository.findByEmail("created-customer@example.com").orElseThrow();
        assertThat(passwordEncoder.matches("StrongPass@123", created.getPassword())).isTrue();
    }

    @Test
    void adminCreatesStaffAndUserAppearsInPaginatedResults() throws Exception {
        String adminToken = tokenFor("admin-create-staff@example.com", Role.ADMIN);

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(
                                        json(
                                                createUserRequest(
                                                        "created-staff@example.com", Role.STAFF))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.role").value("STAFF"));

        mockMvc.perform(
                        get("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .param("search", "created-staff@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].email").value("created-staff@example.com"))
                .andExpect(jsonPath("$.data.content[0].role").value("STAFF"));
    }

    @Test
    void adminCreatesAdmin() throws Exception {
        String adminToken = tokenFor("admin-create-admin@example.com", Role.ADMIN);

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(
                                        json(
                                                createUserRequest(
                                                        "created-admin@example.com", Role.ADMIN))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("created-admin@example.com"))
                .andExpect(jsonPath("$.data.role").value("ADMIN"))
                .andExpect(jsonPath("$.data.enabled").value(true));
    }

    @Test
    void adminCreateRejectsDuplicateEmail() throws Exception {
        String adminToken = tokenFor("admin-create-duplicate@example.com", Role.ADMIN);
        saveUser("duplicate-create@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(
                                        json(
                                                createUserRequest(
                                                        "duplicate-create@example.com",
                                                        Role.STAFF))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void adminCreateRejectsWeakPassword() throws Exception {
        String adminToken = tokenFor("admin-create-weak-password@example.com", Role.ADMIN);
        Map<String, Object> request =
                createUserRequest("weak-password-create@example.com", Role.STAFF);
        request.put("password", "password");

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void adminCreateRejectsInvalidRole() throws Exception {
        String adminToken = tokenFor("admin-create-invalid-role@example.com", Role.ADMIN);
        Map<String, Object> request =
                createUserRequest("invalid-role-create@example.com", Role.STAFF);
        request.put("role", "OWNER");

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(org.hamcrest.Matchers.startsWith("Invalid role")));
    }

    @Test
    void adminUpdatesUserName() throws Exception {
        String adminToken = tokenFor("admin-update-name@example.com", Role.ADMIN);
        User user = saveUser("update-name-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(Map.of("name", "Updated Name"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User updated successfully"))
                .andExpect(jsonPath("$.data.name").value("Updated Name"));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getName())
                .isEqualTo("Updated Name");
    }

    @Test
    void adminUpdatesUserRole() throws Exception {
        String adminToken = tokenFor("admin-update-role@example.com", Role.ADMIN);
        User user = saveUser("update-role-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(Map.of("role", "STAFF"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("STAFF"));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole())
                .isEqualTo(Role.STAFF);
    }

    @Test
    void adminUpdateRejectsInvalidRole() throws Exception {
        String adminToken = tokenFor("admin-update-invalid-role@example.com", Role.ADMIN);
        User user = saveUser("update-invalid-role-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(Map.of("role", "OWNER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value(org.hamcrest.Matchers.startsWith("Invalid role")));

        assertThat(userRepository.findById(user.getId()).orElseThrow().getRole())
                .isEqualTo(Role.CUSTOMER);
    }

    @Test
    void adminUpdatesUserEnabled() throws Exception {
        String adminToken = tokenFor("admin-update-enabled@example.com", Role.ADMIN);
        User user = saveUser("update-enabled-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(Map.of("enabled", false))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false));

        assertThat(userRepository.findById(user.getId()).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    void adminUpdatesUserEmailVerified() throws Exception {
        String adminToken = tokenFor("admin-update-verified@example.com", Role.ADMIN);
        User user = saveUser("update-verified-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(Map.of("emailVerified", true))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.emailVerified").value(true));

        assertThat(userRepository.findById(user.getId()).orElseThrow().isEmailVerified()).isTrue();
    }

    @Test
    void adminCannotDemoteSelf() throws Exception {
        User admin = saveUser("admin-demote-self@example.com", Role.ADMIN);
        saveUser("admin-demote-self-backup@example.com", Role.ADMIN);
        String adminToken = loginToken("admin-demote-self@example.com");

        mockMvc.perform(
                        put("/api/admin/users/{id}", admin.getId())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(Map.of("role", "STAFF"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You cannot remove your own ADMIN role"));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().getRole())
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void cannotDemoteLastEnabledAdmin() throws Exception {
        String adminToken = tokenFor("admin-demote-last@example.com", Role.ADMIN);
        User admin = userRepository.findByEmail("admin-demote-last@example.com").orElseThrow();

        mockMvc.perform(
                        put("/api/admin/users/{id}", admin.getId())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(Map.of("role", "CUSTOMER"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value("Cannot change the last enabled admin to another role"));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().getRole())
                .isEqualTo(Role.ADMIN);
    }

    @Test
    void cannotDisableLastEnabledAdmin() throws Exception {
        User admin = saveUser("admin-disable-last@example.com", Role.ADMIN);
        String adminToken = loginToken("admin-disable-last@example.com");

        mockMvc.perform(
                        put("/api/admin/users/{id}", admin.getId())
                                .header("Authorization", bearer(adminToken))
                                .contentType("application/json")
                                .content(json(Map.of("enabled", false))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot disable the last enabled admin"));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void cannotLockLastEnabledAdmin() throws Exception {
        User admin = saveUser("admin-lock-last@example.com", Role.ADMIN);
        String adminToken = loginToken("admin-lock-last@example.com");

        mockMvc.perform(
                        put("/api/admin/users/{id}/lock", admin.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Cannot lock the last enabled admin"));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().isLocked()).isFalse();
    }

    @Test
    void customerForbiddenFromUserCreationAndUpdate() throws Exception {
        String customerToken = tokenFor("customer-lifecycle-denied@example.com", Role.CUSTOMER);
        User user = saveUser("customer-lifecycle-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", bearer(customerToken))
                                .contentType("application/json")
                                .content(
                                        json(
                                                createUserRequest(
                                                        "customer-forbidden-create@example.com",
                                                        Role.STAFF))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(
                        put("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(customerToken))
                                .contentType("application/json")
                                .content(json(Map.of("name", "Denied"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void staffForbiddenFromUserCreationAndUpdate() throws Exception {
        String staffToken = tokenFor("staff-lifecycle-denied@example.com", Role.STAFF);
        User user = saveUser("staff-lifecycle-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        post("/api/admin/users")
                                .header("Authorization", bearer(staffToken))
                                .contentType("application/json")
                                .content(
                                        json(
                                                createUserRequest(
                                                        "staff-forbidden-create@example.com",
                                                        Role.STAFF))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));

        mockMvc.perform(
                        put("/api/admin/users/{id}", user.getId())
                                .header("Authorization", bearer(staffToken))
                                .contentType("application/json")
                                .content(json(Map.of("name", "Denied"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void unauthenticatedRejectedFromUserCreationAndUpdate() throws Exception {
        User user = saveUser("unauthenticated-lifecycle-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        post("/api/admin/users")
                                .contentType("application/json")
                                .content(
                                        json(
                                                createUserRequest(
                                                        "unauthenticated-create@example.com",
                                                        Role.STAFF))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));

        mockMvc.perform(
                        put("/api/admin/users/{id}", user.getId())
                                .contentType("application/json")
                                .content(json(Map.of("name", "Denied"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void adminEnablesUser() throws Exception {
        String adminToken = tokenFor("admin-enable@example.com", Role.ADMIN);
        User user = saveUser("enable-target@example.com", Role.CUSTOMER);
        user.setEnabled(false);
        user = userRepository.save(user);

        mockMvc.perform(
                        put("/api/admin/users/{id}/enable", user.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User enabled successfully"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.email").value("enable-target@example.com"))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        assertThat(userRepository.findById(user.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void adminDisablesUser() throws Exception {
        String adminToken = tokenFor("admin-disable@example.com", Role.ADMIN);
        User user = saveUser("disable-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}/disable", user.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User disabled successfully"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.password").doesNotExist());

        assertThat(userRepository.findById(user.getId()).orElseThrow().isEnabled()).isFalse();
    }

    @Test
    void adminDisabledUserCannotLogin() throws Exception {
        String adminToken = tokenFor("admin-disable-login@example.com", Role.ADMIN);
        User user = saveUser("disabled-by-admin-login@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}/disable", user.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content(
                                        json(
                                                Map.of(
                                                        "email",
                                                                "disabled-by-admin-login@example.com",
                                                        "password", "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    @Test
    void adminDisabledUsersExistingJwtCannotAccessProtectedEndpoint() throws Exception {
        String adminToken = tokenFor("admin-disable-token@example.com", Role.ADMIN);
        User user = saveUser("disabled-by-admin-token@example.com", Role.CUSTOMER);
        String customerToken = loginToken("disabled-by-admin-token@example.com");

        mockMvc.perform(
                        put("/api/admin/users/{id}/disable", user.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/customer/profile").header("Authorization", bearer(customerToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Account is disabled"));
    }

    @Test
    void adminLocksUser() throws Exception {
        String adminToken = tokenFor("admin-lock@example.com", Role.ADMIN);
        User user = saveUser("lock-target@example.com", Role.CUSTOMER);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(5));
        user = userRepository.save(user);

        mockMvc.perform(
                        put("/api/admin/users/{id}/lock", user.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User locked successfully"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.locked").value(true))
                .andExpect(jsonPath("$.data.lockedUntil").value(nullValue()));

        User locked = userRepository.findById(user.getId()).orElseThrow();
        assertThat(locked.isLocked()).isTrue();
        assertThat(locked.getLockedUntil()).isNull();
    }

    @Test
    void adminLockedUserCannotLogin() throws Exception {
        String adminToken = tokenFor("admin-lock-login@example.com", Role.ADMIN);
        User user = saveUser("locked-by-admin-login@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}/lock", user.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/auth/login")
                                .contentType("application/json")
                                .content(
                                        json(
                                                Map.of(
                                                        "email",
                                                                "locked-by-admin-login@example.com",
                                                        "password", "password123"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(
                        jsonPath("$.message")
                                .value("Account is temporarily locked. Please try again later."));
    }

    @Test
    void adminUnlocksUserAndClearsLockState() throws Exception {
        String adminToken = tokenFor("admin-unlock@example.com", Role.ADMIN);
        User user = saveUser("unlock-target@example.com", Role.CUSTOMER);
        user.setLocked(true);
        user.setFailedLoginAttempts(4);
        user.setLockedUntil(LocalDateTime.now().plusMinutes(10));
        user = userRepository.save(user);

        mockMvc.perform(
                        put("/api/admin/users/{id}/unlock", user.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("User unlocked successfully"))
                .andExpect(jsonPath("$.data.id").value(user.getId()))
                .andExpect(jsonPath("$.data.locked").value(false))
                .andExpect(jsonPath("$.data.failedLoginAttempts").value(0))
                .andExpect(jsonPath("$.data.lockedUntil").value(nullValue()));

        User unlocked = userRepository.findById(user.getId()).orElseThrow();
        assertThat(unlocked.isLocked()).isFalse();
        assertThat(unlocked.getFailedLoginAttempts()).isZero();
        assertThat(unlocked.getLockedUntil()).isNull();
    }

    @Test
    void customerForbiddenFromAccountStateManagement() throws Exception {
        String customerToken = tokenFor("customer-state-denied@example.com", Role.CUSTOMER);
        User user = saveUser("customer-state-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}/disable", user.getId())
                                .header("Authorization", bearer(customerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void staffForbiddenFromAccountStateManagement() throws Exception {
        String staffToken = tokenFor("staff-state-denied@example.com", Role.STAFF);
        User user = saveUser("staff-state-target@example.com", Role.CUSTOMER);

        mockMvc.perform(
                        put("/api/admin/users/{id}/lock", user.getId())
                                .header("Authorization", bearer(staffToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void unauthenticatedRejectedFromAccountStateManagement() throws Exception {
        User user = saveUser("unauthenticated-state-target@example.com", Role.CUSTOMER);

        mockMvc.perform(put("/api/admin/users/{id}/enable", user.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void adminCannotDisableSelf() throws Exception {
        User admin = saveUser("admin-disable-self@example.com", Role.ADMIN);
        saveUser("admin-disable-self-backup@example.com", Role.ADMIN);
        String adminToken = loginToken("admin-disable-self@example.com");

        mockMvc.perform(
                        put("/api/admin/users/{id}/disable", admin.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You cannot disable your own account"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().isEnabled()).isTrue();
    }

    @Test
    void adminCannotLockSelf() throws Exception {
        User admin = saveUser("admin-lock-self@example.com", Role.ADMIN);
        saveUser("admin-lock-self-backup@example.com", Role.ADMIN);
        String adminToken = loginToken("admin-lock-self@example.com");

        mockMvc.perform(
                        put("/api/admin/users/{id}/lock", admin.getId())
                                .header("Authorization", bearer(adminToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("You cannot lock your own account"))
                .andExpect(jsonPath("$.data").value(nullValue()));

        assertThat(userRepository.findById(admin.getId()).orElseThrow().isLocked()).isFalse();
    }

    private User saveNamedUser(String email, String name, Role role) {
        User user = saveUser(email, role);
        user.setName(name);
        return userRepository.save(user);
    }

    private User saveGoogleUser(String email) {
        User user =
                User.builder()
                        .name("Google User")
                        .email(email)
                        .password(null)
                        .role(Role.CUSTOMER)
                        .authProvider(AuthProvider.GOOGLE)
                        .emailVerified(true)
                        .build();
        return userRepository.save(user);
    }

    private Map<String, Object> createUserRequest(String email, Role role) {
        return new java.util.HashMap<>(
                Map.of(
                        "name",
                        role.name() + " Created",
                        "email",
                        email,
                        "password",
                        "StrongPass@123",
                        "role",
                        role.name()));
    }
}
