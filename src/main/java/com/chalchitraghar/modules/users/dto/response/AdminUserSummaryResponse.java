package com.chalchitraghar.modules.users.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Summary response DTO for admin user lists. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserSummaryResponse {

    private Long id;
    private String name;
    private String email;
    private String role;
    private boolean enabled;
    private boolean locked;
}
