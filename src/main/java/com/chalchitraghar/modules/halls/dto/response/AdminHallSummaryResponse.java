package com.chalchitraghar.modules.halls.dto.response;

import com.chalchitraghar.modules.halls.enums.Status;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin list response for cinema halls.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminHallSummaryResponse {
    private Long id;
    private String name;
    private Integer capacity;
    private String layoutRef;
    private Status status;
}
