package com.chalchitraghar.modules.halls.dto.response;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Admin-facing representation of a hall and its generated seat templates. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdminSeatLayoutResponse {

    private Long hallId;
    private String hallName;
    private Integer capacity;
    private Integer totalSeats;
    private Integer premiumSeats;
    private Integer platinumSeats;
    private List<AdminSeatTemplateSummaryResponse> templates;
}
