package com.chalchitraghar.modules.users.repository;

import com.chalchitraghar.modules.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.Optional;

/**
 * Repository interface for User entity persistence operations.
 */
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {
    
    /**
     * Finds a user by email address.
     *
     * @param email the email address to search for
     * @return optional user matching the email
     */
    Optional<User> findByEmail(String email);
    
    /**
     * Checks if a user with the given email already exists.
     *
     * @param email the email address to check
     * @return true if a user with this email exists, false otherwise
     */
    boolean existsByEmail(String email);
}
