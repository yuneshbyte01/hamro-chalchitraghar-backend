package com.chalchitraghar.modules.halls.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chalchitraghar.modules.halls.enums.Status;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request DTO for creating or updating a hall.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallRequest {

    @NotBlank(message = "Name is required")
    @Schema(example = "Hall A", description = "Unique hall name. Leading and trailing whitespace is trimmed.")
    private String name;

    @Positive(message = "Capacity must be at least 1")
    @Max(value = 1000, message = "Capacity must not exceed 1000")
    @NotNull(message = "Capacity is required")
    @Schema(example = "80", minimum = "1", maximum = "1000")
    private Integer capacity;

    @NotBlank(message = "Layout reference is required")
    @Size(max = 100, message = "Layout reference must not exceed 100 characters")
    @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "Layout reference may contain only letters, numbers, hyphen, and underscore")
    @Schema(example = "standard_8x10", maxLength = 100,
            description = "Letters, numbers, hyphen, and underscore only. Leading and trailing whitespace is trimmed.")
    private String layoutRef;

    @NotNull(message = "Status is required")
    @Schema(example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    private Status status;

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public void setLayoutRef(String layoutRef) {
        this.layoutRef = layoutRef == null ? null : layoutRef.trim();
    }
}
