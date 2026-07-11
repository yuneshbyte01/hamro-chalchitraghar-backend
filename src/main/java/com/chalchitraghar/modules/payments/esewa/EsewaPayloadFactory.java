package com.chalchitraghar.modules.payments.esewa;
import org.springframework.stereotype.Component; import com.chalchitraghar.modules.payments.config.EsewaProperties; import com.chalchitraghar.modules.payments.dto.response.EsewaPaymentInitiationResponse; import com.chalchitraghar.modules.payments.entity.Payment;
@Component
public class EsewaPayloadFactory {
 public static final String SIGNED_FIELDS="total_amount,transaction_uuid,product_code";
 private final EsewaProperties p; private final EsewaSignatureService signatures;
 public EsewaPayloadFactory(EsewaProperties p,EsewaSignatureService s){this.p=p;this.signatures=s;}
 public EsewaPaymentInitiationResponse create(Payment x){String amount=signatures.amount(x.getAmount());return new EsewaPaymentInitiationResponse(x.getPaymentReference(),x.getBooking().getBookingReference(),p.paymentUrl().toString(),amount,"0",amount,x.getPaymentReference(),p.productCode(),"0","0",p.successUrl().toString(),p.failureUrl().toString(),SIGNED_FIELDS,signatures.requestSignature(x.getAmount(),x.getPaymentReference()),x.getExpiresAt());}
}
