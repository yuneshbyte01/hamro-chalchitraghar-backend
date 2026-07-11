package com.chalchitraghar.modules.payments.dto.response;
import java.time.LocalDateTime;
public record EsewaPaymentInitiationResponse(String paymentReference,String bookingReference,String paymentUrl,String amount,
 String taxAmount,String totalAmount,String transactionUuid,String productCode,String productServiceCharge,
 String productDeliveryCharge,String successUrl,String failureUrl,String signedFieldNames,String signature,LocalDateTime expiresAt) {}
