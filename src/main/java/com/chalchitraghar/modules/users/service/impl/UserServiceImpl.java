package com.chalchitraghar.modules.users.service.impl;

import com.chalchitraghar.modules.audit.enums.*;
import com.chalchitraghar.modules.audit.service.AuditBusinessPublisher;
import com.chalchitraghar.modules.users.dto.request.AdminCreateUserRequest;
import com.chalchitraghar.modules.users.dto.request.AdminUpdateUserRequest;
import com.chalchitraghar.modules.users.dto.request.AdminUserSearchCriteria;
import com.chalchitraghar.modules.users.dto.response.AdminUserSummaryResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.AuthProvider;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.modules.users.mapper.UserMapper;
import com.chalchitraghar.modules.users.repository.UserRepository;
import com.chalchitraghar.modules.users.service.UserService;
import com.chalchitraghar.modules.users.specification.UserSpecification;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.response.PageResponse;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final List<String> ADMIN_USER_SORT_FIELDS =
            List.of(
                    "id",
                    "name",
                    "email",
                    "role",
                    "enabled",
                    "locked",
                    "authProvider",
                    "createdAt",
                    "updatedAt",
                    "lastLoginAt");

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final AuditBusinessPublisher audit;
    private final UserMapper userMapper;

    @Override
    public User addUser(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user =
                User.builder()
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .role(Role.CUSTOMER)
                        .authProvider(AuthProvider.LOCAL)
                        .emailVerified(false)
                        .passwordChangedAt(LocalDateTime.now())
                        .build();
        return userRepository.save(user);
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public PageResponse<AdminUserSummaryResponse> getAdminUsers(
            AdminUserSearchCriteria criteria, int page, int size, String sortBy, String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
        if (!ADMIN_USER_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sortBy. Allowed values: " + String.join(", ", ADMIN_USER_SORT_FIELDS));
        }

        Sort.Direction direction = parseSortDirection(sortDir);
        Role role = parseEnum(Role.class, criteria.role(), "role");
        AuthProvider authProvider =
                parseEnum(AuthProvider.class, criteria.authProvider(), "authProvider");

        var pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        var users =
                userRepository.findAll(
                        UserSpecification.adminSearch(criteria, role, authProvider), pageable);
        List<AdminUserSummaryResponse> content =
                users.getContent().stream().map(userMapper::toAdminSummaryResponse).toList();

        return PageResponse.from(users, content);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    @Override
    @Transactional
    public User createUser(AdminCreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }
        Role role = parseEnum(Role.class, request.getRole(), "role");
        User user =
                User.builder()
                        .name(request.getName())
                        .email(request.getEmail())
                        .password(passwordEncoder.encode(request.getPassword()))
                        .role(role)
                        .authProvider(AuthProvider.LOCAL)
                        .emailVerified(false)
                        .enabled(true)
                        .locked(false)
                        .failedLoginAttempts(0)
                        .lockedUntil(null)
                        .lastLoginAt(null)
                        .passwordChangedAt(LocalDateTime.now())
                        .build();
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public User updateUser(Long id, AdminUpdateUserRequest request, User currentAdmin) {
        User user = getUserById(id);
        Role roleBefore = user.getRole();
        boolean enabledBefore = user.isEnabled();
        if (request.getName() != null) {
            if (request.getName().isBlank()) {
                throw new IllegalArgumentException("Name is required");
            }
            user.setName(request.getName());
        }
        if (request.getRole() != null) {
            applyRoleChange(user, parseEnum(Role.class, request.getRole(), "role"), currentAdmin);
        }
        if (request.getEnabled() != null) {
            if (Boolean.FALSE.equals(request.getEnabled())) {
                ensureNotLastEnabledAdmin(user, "Cannot disable the last enabled admin");
                ensureNotSelf(user, currentAdmin, "You cannot disable your own account");
            }
            user.setEnabled(request.getEnabled());
        }
        if (request.getEmailVerified() != null) {
            user.setEmailVerified(request.getEmailVerified());
        }
        User saved = userRepository.save(user);
        if (roleBefore != saved.getRole())
            audit.userChange(
                    AuditAction.USER_ROLE_CHANGED,
                    currentAdmin,
                    saved,
                    java.util.Map.of("role", roleBefore.name()),
                    java.util.Map.of("role", saved.getRole().name()),
                    AuditSeverity.HIGH);
        if (enabledBefore != saved.isEnabled())
            audit.userChange(
                    AuditAction.USER_STATUS_CHANGED,
                    currentAdmin,
                    saved,
                    java.util.Map.of("enabled", enabledBefore),
                    java.util.Map.of("enabled", saved.isEnabled()),
                    AuditSeverity.HIGH);
        return saved;
    }

    @Override
    @Transactional
    public User changeRole(User targetUser, Role newRole, User currentAdmin) {
        Role before = targetUser.getRole();
        applyRoleChange(targetUser, newRole, currentAdmin);
        User saved = userRepository.save(targetUser);
        if (before != saved.getRole())
            audit.userChange(
                    AuditAction.USER_ROLE_CHANGED,
                    currentAdmin,
                    saved,
                    java.util.Map.of("role", before.name()),
                    java.util.Map.of("role", saved.getRole().name()),
                    AuditSeverity.HIGH);
        return saved;
    }

    @Override
    public long countEnabledAdmins() {
        return userRepository.countByRoleAndEnabledTrue(Role.ADMIN);
    }

    @Override
    @Transactional
    public User enableUser(Long id, User currentAdmin) {
        User user = getUserById(id);
        boolean before = user.isEnabled();
        user.setEnabled(true);
        User saved = userRepository.save(user);
        if (before != saved.isEnabled())
            audit.userChange(
                    AuditAction.USER_STATUS_CHANGED,
                    currentAdmin,
                    saved,
                    java.util.Map.of("enabled", before),
                    java.util.Map.of("enabled", true),
                    AuditSeverity.HIGH);
        return saved;
    }

    @Override
    @Transactional
    public User disableUser(Long id, User currentAdmin) {
        User user = getUserById(id);
        ensureNotLastEnabledAdmin(user, "Cannot disable the last enabled admin");
        ensureNotSelf(user, currentAdmin, "You cannot disable your own account");
        boolean before = user.isEnabled();
        user.setEnabled(false);
        User saved = userRepository.save(user);
        if (before != saved.isEnabled())
            audit.userChange(
                    AuditAction.USER_STATUS_CHANGED,
                    currentAdmin,
                    saved,
                    java.util.Map.of("enabled", before),
                    java.util.Map.of("enabled", false),
                    AuditSeverity.HIGH);
        return saved;
    }

    @Override
    @Transactional
    public User lockUser(Long id, User currentAdmin) {
        User user = getUserById(id);
        ensureNotLastEnabledAdmin(user, "Cannot lock the last enabled admin");
        ensureNotSelf(user, currentAdmin, "You cannot lock your own account");
        boolean before = user.isLocked();
        user.setLocked(true);
        user.setLockedUntil(null);
        User saved = userRepository.save(user);
        if (before != saved.isLocked())
            audit.userChange(
                    AuditAction.USER_STATUS_CHANGED,
                    currentAdmin,
                    saved,
                    java.util.Map.of("locked", before),
                    java.util.Map.of("locked", true),
                    AuditSeverity.HIGH);
        return saved;
    }

    @Override
    @Transactional
    public User unlockUser(Long id, User currentAdmin) {
        User user = getUserById(id);
        if (isSameUser(user, currentAdmin) && user.isLocked()) {
            throw new IllegalArgumentException("You cannot unlock your own locked account");
        }
        boolean before = user.isLocked();
        user.setLocked(false);
        user.setLockedUntil(null);
        user.setFailedLoginAttempts(0);
        User saved = userRepository.save(user);
        if (before != saved.isLocked())
            audit.userChange(
                    AuditAction.USER_STATUS_CHANGED,
                    currentAdmin,
                    saved,
                    java.util.Map.of("locked", before),
                    java.util.Map.of("locked", false),
                    AuditSeverity.HIGH);
        return saved;
    }

    @Override
    @Transactional
    public User updateCurrentUserProfile(User currentUser, String name) {
        User user = getUserById(currentUser.getId());
        user.setName(name);
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void changeCurrentUserPassword(
            User currentUser, String currentPassword, String newPassword) {
        User user = getUserById(currentUser.getId());
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException(
                    "New password must be different from current password");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setPasswordChangedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private Sort.Direction parseSortDirection(String sortDir) {
        if ("asc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.ASC;
        }
        if ("desc".equalsIgnoreCase(sortDir)) {
            return Sort.Direction.DESC;
        }
        throw new IllegalArgumentException("Invalid sortDir. Allowed values: asc, desc");
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return Arrays.stream(enumType.getEnumConstants())
                .filter(enumValue -> enumValue.name().equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalArgumentException(
                                        "Invalid "
                                                + fieldName
                                                + ". Allowed values: "
                                                + allowedEnumValues(enumType)));
    }

    private <E extends Enum<E>> String allowedEnumValues(Class<E> enumType) {
        return String.join(
                ", ", Arrays.stream(enumType.getEnumConstants()).map(Enum::name).toList());
    }

    private void ensureNotSelf(User targetUser, User currentAdmin, String message) {
        if (isSameUser(targetUser, currentAdmin)) {
            throw new IllegalArgumentException(message);
        }
    }

    private boolean isSameUser(User targetUser, User currentAdmin) {
        return currentAdmin != null
                && targetUser.getId() != null
                && targetUser.getId().equals(currentAdmin.getId());
    }

    private void applyRoleChange(User targetUser, Role newRole, User currentAdmin) {
        if (targetUser.getRole() == newRole) {
            return;
        }
        if (targetUser.getRole() == Role.ADMIN && newRole != Role.ADMIN) {
            ensureNotLastEnabledAdmin(
                    targetUser, "Cannot change the last enabled admin to another role");
            if (isSameUser(targetUser, currentAdmin)) {
                throw new IllegalArgumentException("You cannot remove your own ADMIN role");
            }
        }
        targetUser.setRole(newRole);
    }

    private void ensureNotLastEnabledAdmin(User targetUser, String message) {
        if (targetUser.getRole() == Role.ADMIN
                && targetUser.isEnabled()
                && countEnabledAdmins() <= 1) {
            throw new IllegalArgumentException(message);
        }
    }
}
