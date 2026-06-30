package com.chalchitraghar.modules.halls;

import lombok.AllArgsConstructor;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.chalchitraghar.modules.halls.Status;

/**
 * Response DTO containing hall information.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HallResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private String layoutRef;   
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
