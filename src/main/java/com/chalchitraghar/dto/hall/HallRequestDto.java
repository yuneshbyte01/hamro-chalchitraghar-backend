package com.chalchitraghar.dto.hall;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.NoArgsConstructor;
import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallRequestDto {

    @NotBlank(message = "Name is required")
    private String name;

    @PositiveOrZero(message = "Capacity must be at least 1")
    @NotNull(message = "Capacity is required")
    private Integer capacity;

    @NotBlank(message = "Layout reference is required")
    private String layoutRef;
    
    @NotNull(message = "Active status is required")
    private boolean isActive;
}
