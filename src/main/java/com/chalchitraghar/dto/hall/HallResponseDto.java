package com.chalchitraghar.dto.hall;

import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallResponseDto {
    private Long id;
    private String name;
    private Integer capacity;
    private String layoutRef;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
