package com.chalchitraghar.modules.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {

    @NotBlank(message = "Google ID token is required")
    @Schema(example = "GOOGLE_ID_TOKEN")
    private String idToken;
}
