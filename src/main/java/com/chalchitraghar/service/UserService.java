package com.chalchitraghar.service;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.chalchitraghar.exception.ResourceNotFoundException;
import com.chalchitraghar.model.User;
import com.chalchitraghar.model.enums.Role;
import com.chalchitraghar.repository.UserRepository;

import lombok.RequiredArgsConstructor;

/**
 * Service for user management operations.
 */
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    
    /**
     * Adds a new user to the system with CUSTOMER role by default.
     *
     * @param name user name
     * @param email user email (must be unique)
     * @param password plain text password (will be encrypted)
     * @return saved user entity
     * @throws IllegalArgumentException if email is already registered
     */
    public User addUser(String name, String email, String password) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        User user = User.builder()
                .name(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(Role.CUSTOMER)
                .build();

        return userRepository.save(user);
    }

    /**
     * Retrieves a user by email address.
     *
     * @param email the email address
     * @return user entity
     * @throws RuntimeException if user is not found
     */
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found with email: " + email));
    }

    /**
     * Retrieves all users. For admin use.
     *
     * @return list of all users
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Retrieves a user by ID.
     *
     * @param id the user ID
     * @return user entity
     * @throws ResourceNotFoundException if user is not found
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", id));
    }
}
