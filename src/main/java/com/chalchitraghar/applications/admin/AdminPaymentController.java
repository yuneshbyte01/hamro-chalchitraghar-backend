package com.chalchitraghar.applications.admin;
import java.util.List; import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*;
import com.chalchitraghar.modules.payments.dto.response.*; import com.chalchitraghar.modules.payments.service.PaymentService; import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag; import lombok.RequiredArgsConstructor;
import com.chalchitraghar.modules.payments.esewa.EsewaVerificationService;
import com.chalchitraghar.modules.payments.mapper.PaymentMapper;
import com.chalchitraghar.modules.payments.service.PaymentOperationsService;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.shared.response.PageResponse;
@RestController @RequestMapping("/api/admin") @RequiredArgsConstructor @Tag(name="Admin Payments", description="Read-only Payment-1 administrative payment endpoints") @SecurityRequirement(name="bearerAuth")
public class AdminPaymentController {
 private final PaymentService service;
 private final EsewaVerificationService esewaVerificationService;
 private final PaymentMapper paymentMapper;
 private final PaymentOperationsService operations;
 @GetMapping("/payments") @Operation(summary="Search payments") public ResponseEntity<ApiResponse<PageResponse<AdminPaymentSummaryResponse>>> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="createdAt") String sortBy,@RequestParam(defaultValue="desc") String sortDir,@RequestParam(required=false) PaymentProvider provider,@RequestParam(required=false) PaymentStatus status,@RequestParam(required=false) String search){return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully",operations.list(page,size,sortBy,sortDir,provider,status,search)));}
 @GetMapping("/payments/manual-review") @Operation(summary="List manual-review payments") public ResponseEntity<ApiResponse<PageResponse<AdminPaymentSummaryResponse>>> review(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return ResponseEntity.ok(ApiResponse.success("Manual review queue fetched",operations.manualReview(page,size)));}
 @PostMapping("/payments/{reference}/resolve") @Operation(summary="Resolve manual review") public ResponseEntity<ApiResponse<AdminPaymentDetailResponse>> resolve(@PathVariable String reference,@RequestParam ManualReviewResolution resolution){return ResponseEntity.ok(ApiResponse.success("Manual review updated",operations.resolve(reference,resolution)));}
 @GetMapping("/payments/statistics") @Operation(summary="Payment statistics") public ResponseEntity<ApiResponse<PaymentStatisticsResponse>> statistics(){return ResponseEntity.ok(ApiResponse.success("Payment statistics fetched",operations.statistics()));}
 @GetMapping("/payments/consistency") @Operation(summary="Payment consistency issues") public ResponseEntity<ApiResponse<List<PaymentConsistencyIssue>>> consistency(){return ResponseEntity.ok(ApiResponse.success("Payment consistency checked",operations.consistency()));}
 @GetMapping("/payments/{reference}") @Operation(summary="Get payment by server-generated reference") public ResponseEntity<ApiResponse<AdminPaymentDetailResponse>> get(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully",service.getAdminPaymentByReference(reference)));}
 @GetMapping("/bookings/{reference}/payments") @Operation(summary="List booking payment history") public ResponseEntity<ApiResponse<List<AdminPaymentSummaryResponse>>> byBooking(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully",service.getAdminPaymentsByBookingReference(reference)));}
 @PostMapping("/payments/{reference}/reconcile") @Operation(summary="Reconcile an eSewa payment") public ResponseEntity<ApiResponse<AdminPaymentDetailResponse>> reconcile(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payment reconciled successfully",paymentMapper.toAdminDetail(esewaVerificationService.reconcile(reference))));}
}
