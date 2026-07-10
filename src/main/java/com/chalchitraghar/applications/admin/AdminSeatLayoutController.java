package com.chalchitraghar.applications.admin;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.chalchitraghar.modules.halls.service.SeatLayoutService;
import com.chalchitraghar.modules.halls.dto.response.AdminSeatLayoutResponse;
import com.chalchitraghar.shared.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * REST controller for admin seat layout management. Admin only.
 */
@RestController
@RequestMapping("/api/admin/halls")
@RequiredArgsConstructor
@Tag(name = "Admin Halls", description = "Admin hall seat layout management")
@SecurityRequirement(name = "bearerAuth")
public class AdminSeatLayoutController {

    private final SeatLayoutService seatLayoutService;

    @GetMapping("/{hallId}/seat-layout")
    @Operation(summary = "Get generated seat layout for a hall",
            description = "Returns the complete generated seat template layout ordered by positionIndex.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat layout fetched",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = AdminSeatLayoutResponse.class),
                    examples = @ExampleObject(value = """
                            {"success":true,"message":"Seat layout fetched successfully","data":{"hallId":1,"hallName":"Hall A","capacity":188,"totalSeats":188,"premiumSeats":8,"platinumSeats":180,"templates":[{"id":1,"rowLabel":"A","seatNumber":1,"seatCode":"A1","seatType":"PREMIUM","positionIndex":0}]},"errors":[]}
                            """)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
            description = "Hall not found or hall has no generated seat layout")
    public ResponseEntity<ApiResponse<AdminSeatLayoutResponse>> getSeatLayout(@PathVariable Long hallId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Seat layout fetched successfully", seatLayoutService.getSeatLayout(hallId)));
    }

    /**
     * Generates seat layout templates for a hall.
     *
     * @param hallId the hall ID
     * @return success response
    */
    @PostMapping("/{hallId}/seat-layout")
    @Operation(
            summary = "Generate seat layout for a hall",
            description = """
                    Generates the fixed 188-seat layout for an ACTIVE hall.
                    The hall must not already have seat templates, and current hall capacity must be 188.""")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Seat layout generated",
            content = @Content(schema = @Schema(implementation = Void.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Hall not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Inactive hall, duplicate seat layout, or hall capacity does not match the fixed 188-seat generator")
    public ResponseEntity<ApiResponse<Void>> generateSeatLayout(@PathVariable Long hallId) {
        seatLayoutService.generateSeatTemplates(hallId);
        return ResponseEntity.ok(ApiResponse.success("Seat layout generated successfully"));
    }
}
