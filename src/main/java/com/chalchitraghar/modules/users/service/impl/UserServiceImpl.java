package com.chalchitraghar.modules.users.service.impl;

import com.chalchitraghar.modules.users.service.UserService;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.response.PageResponse;
import com.chalchitraghar.modules.users.dto.request.AdminUserSearchCriteria;
import com.chalchitraghar.modules.users.dto.response.AdminUserSummaryResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.AuthProvider;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.modules.users.mapper.UserMapper;
import com.chalchitraghar.modules.users.repository.UserRepository;
import com.chalchitraghar.modules.users.specification.UserSpecification;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final List<String> ADMIN_USER_SORT_FIELDS = List.of(
            "id",
            "name",
            "email",
            "role",
            "enabled",
            "locked",
            "authProvider",
            "createdAt",
            "updatedAt",
            "lastLoginAt"
    );

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    @Override
    public User addUser(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }
        User user = User.builder()
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
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public PageResponse<AdminUserSummaryResponse> getAdminUsers(
            AdminUserSearchCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortDir) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Size must be at least 1");
        }
        if (!ADMIN_USER_SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Invalid sortBy. Allowed values: "
                    + String.join(", ", ADMIN_USER_SORT_FIELDS));
        }

        Sort.Direction direction = parseSortDirection(sortDir);
        Role role = parseEnum(Role.class, criteria.role(), "role");
        AuthProvider authProvider = parseEnum(AuthProvider.class, criteria.authProvider(), "authProvider");

        var pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        var users = userRepository.findAll(
                UserSpecification.adminSearch(criteria, role, authProvider),
                pageable);
        List<AdminUserSummaryResponse> content = users.getContent().stream()
                .map(userMapper::toAdminSummaryResponse)
                .toList();

        return PageResponse.from(users, content);
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
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
    public void changeCurrentUserPassword(User currentUser, String currentPassword, String newPassword) {
        User user = getUserById(currentUser.getId());
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from current password");
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
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid " + fieldName + ". Allowed values: " + allowedEnumValues(enumType)));
    }

    private <E extends Enum<E>> String allowedEnumValues(Class<E> enumType) {
        return String.join(", ", Arrays.stream(enumType.getEnumConstants())
                .map(Enum::name)
                .toList());
    }
}
