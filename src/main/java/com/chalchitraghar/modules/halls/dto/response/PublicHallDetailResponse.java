package com.chalchitraghar.modules.halls.dto.response;

import com.chalchitraghar.modules.halls.enums.Status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public detail response for an active cinema hall.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicHallDetailResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private Status status;
}
