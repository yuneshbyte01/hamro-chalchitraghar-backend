package com.chalchitraghar.applications.admin;
import java.util.List; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import com.chalchitraghar.modules.payments.dto.response.*; import com.chalchitraghar.modules.payments.service.PaymentService; import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag; import lombok.RequiredArgsConstructor;
import com.chalchitraghar.modules.payments.esewa.EsewaVerificationService;
import com.chalchitraghar.modules.payments.mapper.PaymentMapper;
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor @Tag(name="Admin Payments", description="Read-only Payment-1 administrative payment endpoints") @SecurityRequirement(name="bearerAuth")
public class AdminPaymentController {
 private final PaymentService service;
 private final EsewaVerificationService esewaVerificationService;
 private final PaymentMapper paymentMapper;
 @GetMapping("/payments/{reference}") @Operation(summary="Get payment by server-generated reference") public ResponseEntity<ApiResponse<AdminPaymentDetailResponse>> get(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully",service.getAdminPaymentByReference(reference)));}
 @GetMapping("/bookings/{reference}/payments") @Operation(summary="List booking payment history") public ResponseEntity<ApiResponse<List<AdminPaymentSummaryResponse>>> byBooking(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully",service.getAdminPaymentsByBookingReference(reference)));}
 @PostMapping("/payments/{reference}/reconcile") @Operation(summary="Reconcile an eSewa payment") public ResponseEntity<ApiResponse<AdminPaymentDetailResponse>> reconcile(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payment reconciled successfully",paymentMapper.toAdminDetail(esewaVerificationService.reconcile(reference))));}
}
