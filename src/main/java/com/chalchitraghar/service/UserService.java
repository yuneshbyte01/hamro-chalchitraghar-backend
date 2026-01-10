package com.chalchitraghar.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.chalchitraghar.model.User;
import com.chalchitraghar.model.enums.Role;
import com.chalchitraghar.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository; // Repository for user operations
    private final BCryptPasswordEncoder passwordEncoder; // Password encoder for user passwords

    public UserService (UserRepository userRepository, BCryptPasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Registers a new customer user.
     * @param name User name
     * @param email User email (must be unique)
     * @param password Plain text password
     * @return Saved User
     */
    public User registerUser(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.CUSTOMER) // Default role
                .createdAt(now)
                .updatedAt(now)
                .build();

        return userRepository.save(user);
    }
}
