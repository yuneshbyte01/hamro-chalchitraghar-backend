package com.chalchitraghar.service.admin;

import java.util.List;

import com.chalchitraghar.model.User;

/**
 * Service for user management operations.
 */
public interface UserService {

    User addUser(String name, String email, String password);

    User getUserByEmail(String email);

    List<User> getAllUsers();

    User getUserById(Long id);
}
