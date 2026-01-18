package com.chalchitraghar.dto.hall;

import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chalchitraghar.model.enums.Status;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallResponseDto {
    private Long id;
    private String name;
    private Integer capacity;
    private String layoutRef;   
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
