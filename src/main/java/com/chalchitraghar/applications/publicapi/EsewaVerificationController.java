package com.chalchitraghar.applications.publicapi;
import org.springframework.http.ResponseEntity; import org.springframework.web.bind.annotation.*; import jakarta.validation.Valid;
import com.chalchitraghar.modules.payments.dto.request.EsewaVerificationRequest; import com.chalchitraghar.modules.payments.dto.response.CustomerPaymentDetailResponse;
import com.chalchitraghar.modules.payments.esewa.EsewaVerificationService; import com.chalchitraghar.shared.response.ApiResponse; import lombok.RequiredArgsConstructor;
import io.swagger.v3.oas.annotations.Operation; import io.swagger.v3.oas.annotations.tags.Tag;
@RestController @RequestMapping("/api/payments/esewa") @RequiredArgsConstructor @Tag(name="eSewa Verification",description="Signed browser-redirect verification; not a webhook")
public class EsewaVerificationController {
 private final EsewaVerificationService service;
 @PostMapping("/verify") @Operation(summary="Verify signed eSewa success response",description="Validates the Base64 redirect signature and independently checks transaction status before finalization.")
 public ResponseEntity<ApiResponse<CustomerPaymentDetailResponse>> verify(@Valid @RequestBody EsewaVerificationRequest request){return ResponseEntity.ok(ApiResponse.success("eSewa payment verified",service.verify(request.data())));}
}
