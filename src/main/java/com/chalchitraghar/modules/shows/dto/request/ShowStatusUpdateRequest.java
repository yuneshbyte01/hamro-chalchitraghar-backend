package com.chalchitraghar.modules.shows.dto.request;

import com.chalchitraghar.modules.shows.enums.ShowStatus;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Administrative show status transition request")
public class ShowStatusUpdateRequest {

    @NotNull(message = "Status is required")
    @Schema(example = "RUNNING")
    private ShowStatus status;
}
