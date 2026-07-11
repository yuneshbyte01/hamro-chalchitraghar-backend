package com.chalchitraghar.applications.customer;
import java.util.List;
import org.springframework.http.ResponseEntity; import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import com.chalchitraghar.modules.payments.dto.response.*; import com.chalchitraghar.modules.payments.service.PaymentService;
import com.chalchitraghar.modules.users.entity.User; import com.chalchitraghar.shared.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import com.chalchitraghar.modules.payments.dto.request.PaymentInitiationRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import io.swagger.v3.oas.annotations.Parameter;
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
    @PostMapping("/bookings/{reference}/payments")
    @Operation(summary="Initiate a payment attempt", description="Amount/currency and ownership are server-authoritative. Idempotent per booking and key; one active attempt is allowed. Payment success does not confirm the booking.")
    public ResponseEntity<ApiResponse<CustomerPaymentDetailResponse>> initiate(@PathVariable String reference,
            @Parameter(required=true,description="Client-generated key, unique per booking; maximum 255 characters") @RequestHeader("Idempotency-Key") String key,
            @Valid @RequestBody PaymentInitiationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success("Payment initiated successfully",service.initiate(reference,key,request,user())));
    }
    @PostMapping("/payments/{reference}/cancel")
    @Operation(summary="Cancel an active payment", description="Owner-only. Already-cancelled payments return their existing representation; other terminal states conflict.")
    public ResponseEntity<ApiResponse<CustomerPaymentDetailResponse>> cancel(@PathVariable String reference){return ResponseEntity.ok(ApiResponse.success("Payment cancelled successfully",service.cancel(reference,user())));}
    private User user() { return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal(); }
}
