package com.chalchitraghar.applications.admin;

import com.chalchitraghar.modules.payments.dto.response.AdminRefundSummaryReport;
import com.chalchitraghar.modules.payments.service.RefundReportService;
import com.chalchitraghar.shared.response.*;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/refund-reports")
@RequiredArgsConstructor
@Tag(name = "Admin Refund Reports")
@SecurityRequirement(name = "bearerAuth")
public class AdminRefundReportController {
    private final RefundReportService reports;

    @GetMapping("/summary")
    @Operation(
            summary = "Get bounded refund financial summary",
            description =
                    "Database aggregation grouped by status and currency; currencies are never combined into an unlabeled total.")
    public ResponseEntity<ApiResponse<AdminRefundSummaryReport>> summary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to) {
        return ResponseEntity.ok(
                ApiResponse.success("Refund report generated", reports.summary(from, to)));
    }
}
