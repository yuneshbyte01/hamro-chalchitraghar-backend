package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.halls.dto.request.SeatTemplateSearchCriteria;
import com.chalchitraghar.modules.halls.dto.response.AdminSeatLayoutResponse;
import com.chalchitraghar.modules.halls.service.SeatLayoutService;
import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** REST controller for admin seat layout management. Admin only. */
@RestController
@RequestMapping("/api/admin/halls")
@RequiredArgsConstructor
@Tag(name = "Admin Halls", description = "Admin hall seat layout management")
@SecurityRequirement(name = "bearerAuth")
public class AdminSeatLayoutController {

    private final SeatLayoutService seatLayoutService;

    @GetMapping("/{hallId}/seat-layout")
    @Operation(
            summary = "Get generated seat layout for a hall",
            description =
                    "Returns generated seat templates with optional case-insensitive search, filters, and sorting. Statistics describe the returned templates.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Seat layout fetched",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AdminSeatLayoutResponse.class),
                            examples =
                                    @ExampleObject(
                                            value =
                                                    """
                            {"success":true,"message":"Seat layout fetched successfully","data":{"hallId":1,"hallName":"Hall A","capacity":188,"totalSeats":188,"premiumSeats":8,"platinumSeats":180,"rows":["A","B","C","D","E","F","G","H","I","J"],"templates":[{"id":1,"rowLabel":"A","seatNumber":1,"seatCode":"A1","seatType":"PREMIUM","positionIndex":0}]},"errors":[]}
                            """)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Hall not found or hall has no generated seat layout")
    public ResponseEntity<ApiResponse<AdminSeatLayoutResponse>> getSeatLayout(
            @PathVariable Long hallId,
            @Parameter(description = "Case-insensitive match against seatCode or rowLabel")
                    @RequestParam(required = false)
                    String search,
            @Parameter(description = "Filter by PREMIUM or PLATINUM")
                    @RequestParam(required = false)
                    String seatType,
            @Parameter(description = "Case-insensitive exact row label")
                    @RequestParam(required = false)
                    String row,
            @Parameter(
                            description =
                                    "Sort field: positionIndex, rowLabel, seatNumber, seatCode, seatType")
                    @RequestParam(defaultValue = "positionIndex")
                    String sortBy,
            @Parameter(description = "Sort direction: asc or desc")
                    @RequestParam(defaultValue = "asc")
                    String sortDir) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Seat layout fetched successfully",
                        seatLayoutService.getSeatLayout(
                                hallId,
                                new SeatTemplateSearchCriteria(
                                        search, seatType, row, sortBy, sortDir))));
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
            description =
                    """
                    Generates the fixed 188-seat layout for an ACTIVE hall.
                    The hall must not already have seat templates, and current hall capacity must be 188.
                    The complete preset is validated for uppercase rows, sequential seat numbers and positions,
                    generated seat codes, supported categories, and capacity consistency before bulk persistence.""")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Seat layout generated",
            content = @Content(schema = @Schema(implementation = Void.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Hall not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description =
                    "Inactive hall, duplicate seat layout, or hall capacity does not match the fixed 188-seat generator")
    public ResponseEntity<ApiResponse<Void>> generateSeatLayout(@PathVariable Long hallId) {
        seatLayoutService.generateSeatTemplates(hallId);
        return ResponseEntity.ok(ApiResponse.success("Seat layout generated successfully"));
    }

    @PostMapping("/{hallId}/seat-layout/regenerate")
    @Operation(
            summary = "Safely regenerate a hall seat layout",
            description =
                    "Recreates the fixed validated layout only for an ACTIVE supported hall that already has a layout and has no shows of any status.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "Seat layout regenerated",
            content = @Content(schema = @Schema(implementation = AdminSeatLayoutResponse.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "Hall or existing layout not found")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            description = "Inactive/unsupported hall or shows exist")
    public ResponseEntity<ApiResponse<AdminSeatLayoutResponse>> regenerateSeatLayout(
            @PathVariable Long hallId) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        "Seat layout regenerated successfully",
                        seatLayoutService.regenerateSeatTemplates(hallId)));
    }
}
