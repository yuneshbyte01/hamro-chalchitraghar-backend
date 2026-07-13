package com.chalchitraghar.modules.users.service;

import com.chalchitraghar.modules.users.dto.request.AdminCreateUserRequest;
import com.chalchitraghar.modules.users.dto.request.AdminUpdateUserRequest;
import com.chalchitraghar.modules.users.dto.request.AdminUserSearchCriteria;
import com.chalchitraghar.modules.users.dto.response.AdminUserSummaryResponse;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.enums.Role;
import com.chalchitraghar.shared.response.PageResponse;
import java.util.List;

/** Service for user management operations. */
public interface UserService {

    User addUser(String name, String email, String password);

    User getUserByEmail(String email);

    List<User> getAllUsers();

    PageResponse<AdminUserSummaryResponse> getAdminUsers(
            AdminUserSearchCriteria criteria, int page, int size, String sortBy, String sortDir);

    User getUserById(Long id);

    User createUser(AdminCreateUserRequest request);

    User updateUser(Long id, AdminUpdateUserRequest request, User currentAdmin);

    User changeRole(User targetUser, Role newRole, User currentAdmin);

    long countEnabledAdmins();

    User enableUser(Long id, User currentAdmin);

    User disableUser(Long id, User currentAdmin);

    User lockUser(Long id, User currentAdmin);

    User unlockUser(Long id, User currentAdmin);

    User updateCurrentUserProfile(User currentUser, String name);

    void changeCurrentUserPassword(User currentUser, String currentPassword, String newPassword);
}
