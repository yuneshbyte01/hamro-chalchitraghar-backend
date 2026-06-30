package com.chalchitraghar.modules.halls;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chalchitraghar.modules.halls.Status;

/**
 * Request DTO for creating or updating a hall.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @PositiveOrZero(message = "Capacity must be at least 1")
    @NotNull(message = "Capacity is required")
    private Integer capacity;

    @NotBlank(message = "Layout reference is required")
    private String layoutRef;

    @NotNull(message = "Status is required")
    private Status status;
}
