package com.chalchitraghar.modules.halls.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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
    @Schema(example = "Hall A")
    private String name;

    @PositiveOrZero(message = "Capacity must be at least 1")
    @NotNull(message = "Capacity is required")
    @Schema(example = "80")
    private Integer capacity;

    @NotBlank(message = "Layout reference is required")
    @Schema(example = "8x10")
    private String layoutRef;

    @NotNull(message = "Status is required")
    @Schema(example = "ACTIVE")
    private Status status;
}
