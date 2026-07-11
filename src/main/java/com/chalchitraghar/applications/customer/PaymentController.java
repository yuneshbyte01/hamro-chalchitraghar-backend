package com.chalchitraghar.applications.customer;
import java.util.List;
import org.springframework.http.ResponseEntity; import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.chalchitraghar.modules.payments.dto.response.*; import com.chalchitraghar.modules.payments.service.PaymentService;
import com.chalchitraghar.modules.users.entity.User; import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
@RestController @RequestMapping("/api/customer") @RequiredArgsConstructor
@Tag(name="Customer Payments", description="Read-only Payment-1 endpoints; payment initiation is not available") @SecurityRequirement(name="bearerAuth")
public class PaymentController {
    private final PaymentService service;
    @GetMapping("/payments") @Operation(summary="List my payments", description="Returns only payment records owned by the authenticated customer.")
    public ResponseEntity<ApiResponse<List<CustomerPaymentSummaryResponse>>> list() { return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", service.getMyPayments(user()))); }
    @GetMapping("/payments/{reference}") @Operation(summary="Get my payment by server-generated reference")
    public ResponseEntity<ApiResponse<CustomerPaymentDetailResponse>> get(@PathVariable String reference) { return ResponseEntity.ok(ApiResponse.success("Payment fetched successfully", service.getMyPaymentByReference(reference,user()))); }
    @GetMapping("/bookings/{reference}/payments") @Operation(summary="List my booking payment history", description="Owner-only, read-only history using a booking reference.")
    public ResponseEntity<ApiResponse<List<CustomerPaymentSummaryResponse>>> byBooking(@PathVariable String reference) { return ResponseEntity.ok(ApiResponse.success("Payments fetched successfully", service.getMyPaymentsByBookingReference(reference,user()))); }
    private User user() { return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}
