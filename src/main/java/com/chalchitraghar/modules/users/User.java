package com.chalchitraghar.modules.users;

import com.chalchitraghar.shared.GenericEntity;

import com.chalchitraghar.modules.users.Role;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Represents a user account with authentication credentials and role-based access.
 */
@Entity
@Table(name = "users")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User extends GenericEntity {

    /**
     * Name of the user. Must be not blank.
     */
    @Column(nullable = false)
    @NotBlank(message = "Name is required")
    private String name;

    /**
     * Unique email address used for authentication and identification.
     */
    @Column(nullable = false, unique = true)
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email address")
    private String email;

    /**
     * Encrypted password. Must be at least 8 characters long.
     */
    @Column(nullable = false)
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters long")
    private String password;

    /**
     * Role of the user. Must be not null.
     * CUSTOMER, STAFF, ADMIN
     */
    @Column(nullable = false)
    @NotNull(message = "Role is required")
    @Enumerated(EnumType.STRING)
    private Role role;
}
