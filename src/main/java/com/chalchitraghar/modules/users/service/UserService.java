package com.chalchitraghar.modules.users.service;

import java.util.List;

import com.chalchitraghar.modules.users.dto.request.AdminUserSearchCriteria;
import com.chalchitraghar.modules.users.dto.response.AdminUserSummaryResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.response.PageResponse;

/**
 * Service for user management operations.
 */
public interface UserService {

    User addUser(String name, String email, String password);

    User getUserByEmail(String email);

    List<User> getAllUsers();

    PageResponse<AdminUserSummaryResponse> getAdminUsers(
            AdminUserSearchCriteria criteria,
            int page,
            int size,
            String sortBy,
            String sortDir);

    User getUserById(Long id);

    User enableUser(Long id, User currentAdmin);

    User disableUser(Long id, User currentAdmin);

    User lockUser(Long id, User currentAdmin);

    User unlockUser(Long id, User currentAdmin);

    User updateCurrentUserProfile(User currentUser, String name);

    void changeCurrentUserPassword(User currentUser, String currentPassword, String newPassword);
}
