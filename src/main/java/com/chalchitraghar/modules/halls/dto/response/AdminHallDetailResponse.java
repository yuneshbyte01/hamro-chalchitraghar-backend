package com.chalchitraghar.modules.halls.dto.response;

import java.time.LocalDateTime;

import com.chalchitraghar.modules.halls.enums.Status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin detail response for cinema halls.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminHallDetailResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private String layoutRef;
    private Status status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
