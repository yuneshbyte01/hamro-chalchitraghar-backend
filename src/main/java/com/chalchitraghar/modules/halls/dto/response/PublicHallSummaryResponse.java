package com.chalchitraghar.modules.halls.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Public list response for active cinema halls.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublicHallSummaryResponse {
    private Long id;
    private String name;
    private Integer capacity;
}
