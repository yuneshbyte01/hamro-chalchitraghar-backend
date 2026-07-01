package com.chalchitraghar.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for token refresh.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    @NotBlank(message = "Token is required")
    @Schema(example = "eyJhbGciOiJIUzI1NiJ9.refresh-token-value")
    private String token;
}
