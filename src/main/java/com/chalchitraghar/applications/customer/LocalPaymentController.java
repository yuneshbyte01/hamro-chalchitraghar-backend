package com.chalchitraghar.applications.customer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity; import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*; import jakarta.validation.Valid;
import com.chalchitraghar.modules.payments.dto.request.LocalPaymentProcessRequest;
import com.chalchitraghar.modules.payments.dto.response.CustomerPaymentDetailResponse;
import com.chalchitraghar.modules.payments.service.PaymentService; import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.response.ApiResponse; import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.security.SecurityRequirement; import io.swagger.v3.oas.annotations.tags.Tag;
@RestController @RequestMapping("/api/customer/payments") @RequiredArgsConstructor
@ConditionalOnProperty(name="app.payments.local-enabled",havingValue="true")
@Tag(name="Local Payments",description="Development/test-only LOCAL payment simulator") @SecurityRequirement(name="bearerAuth")
public class LocalPaymentController {
 private final PaymentService service;
 @PostMapping("/{reference}/process") @Operation(summary="Simulate LOCAL payment result",description="Enabled only when PAYMENT_LOCAL_ENABLED=true; never enabled by default in production.")
 public ResponseEntity<ApiResponse<CustomerPaymentDetailResponse>> process(@PathVariable String reference,@Valid @RequestBody LocalPaymentProcessRequest request){User u=(User)SecurityContextHolder.getContext().getAuthentication().getPrincipal();return ResponseEntity.ok(ApiResponse.success("Local payment processed successfully",service.processLocal(reference,request,u)));}
}
