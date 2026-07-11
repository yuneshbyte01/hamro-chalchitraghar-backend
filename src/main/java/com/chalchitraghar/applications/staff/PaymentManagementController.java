package com.chalchitraghar.applications.staff;
import java.util.List; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import com.chalchitraghar.modules.payments.dto.response.*; import com.chalchitraghar.modules.payments.service.PaymentService; import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag; import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/staff") @RequiredArgsConstructor @Tag(name="Staff Payments", description="Read-only Payment-1 operational payment endpoints") @SecurityRequirement(name="bearerAuth")
public class PaymentManagementController {
 private final PaymentService service;
 @GetMapping("/payments/{reference}") @Operation(summary="Get payment by server-generated reference") public ResponseEntity<ApiResponse<StaffPaymentDetailResponse>> get(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully",service.getStaffPaymentByReference(reference)));}
 @GetMapping("/bookings/{reference}/payments") @Operation(summary="List booking payment history") public ResponseEntity<ApiResponse<List<StaffPaymentSummaryResponse>>> byBooking(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully",service.getStaffPaymentsByBookingReference(reference)));}
}
