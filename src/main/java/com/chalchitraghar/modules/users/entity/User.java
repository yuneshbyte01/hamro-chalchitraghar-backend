package com.chalchitraghar.modules.users.entity;

import com.chalchitraghar.modules.users.enums.AuthProvider;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.shared.GenericEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/** Represents a user account with authentication credentials and role-based access. */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends GenericEntity {

    /** Name of the user. Must be not blank. */
    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    /** Unique email address used for authentication and identification. */
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    /** Encrypted password. Nullable for Google-only accounts. */
    @Column private String password;

    /** Role of the user. Must be not null. CUSTOMER, STAFF, ADMIN */
    @Column(nullable = false)
    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    private Role role;

    @Column(nullable = false, length = 20)
    @NotNull(message = "Auth provider is required")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column private String googleId;

    @Column(nullable = false)
    @Builder.Default
    private boolean emailVerified = false;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean locked = false;

    @Column(nullable = false)
    @Builder.Default
    private int failedLoginAttempts = 0;

    @Column private LocalDateTime lockedUntil;

    @Column private LocalDateTime lastLoginAt;

    @Column private LocalDateTime passwordChangedAt;
}
