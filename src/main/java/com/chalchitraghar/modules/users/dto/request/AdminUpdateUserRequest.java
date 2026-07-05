package com.chalchitraghar.modules.users.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class AdminUpdateUserRequest {

    @Schema(example = "John Doe")
    private String name;

    @Schema(example = "STAFF", allowableValues = {"CUSTOMER", "STAFF", "ADMIN"})
    private String role;

    @Schema(example = "true")
    private Boolean enabled;

    @Schema(example = "false")
    private Boolean emailVerified;
}
