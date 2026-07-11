package com.chalchitraghar.modules.payments.esewa;
import java.math.BigDecimal; import java.nio.charset.StandardCharsets; import java.security.MessageDigest; import java.util.Base64;
import javax.crypto.Mac; import javax.crypto.spec.SecretKeySpec; import org.springframework.stereotype.Service;
import com.chalchitraghar.modules.payments.config.EsewaProperties; import com.chalchitraghar.shared.exception.PaymentConflictException;
@Service
public class EsewaSignatureService {
 private final EsewaProperties properties; public EsewaSignatureService(EsewaProperties p){this.properties=p;}
 public String amount(BigDecimal value){return value.stripTrailingZeros().toPlainString();}
 public String sign(String message){try{Mac mac=Mac.getInstance("HmacSHA256");mac.init(new SecretKeySpec(properties.secretKey().getBytes(StandardCharsets.UTF_8),"HmacSHA256"));return Base64.getEncoder().encodeToString(mac.doFinal(message.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException("Could not sign eSewa request",e);}}
 public String requestSignature(BigDecimal total,String uuid){return sign("total_amount="+amount(total)+",transaction_uuid="+uuid+",product_code="+properties.productCode());}
 public void verify(String message,String supplied){byte[] a=sign(message).getBytes(StandardCharsets.US_ASCII);byte[] b=supplied.getBytes(StandardCharsets.US_ASCII);if(!MessageDigest.isEqual(a,b))throw new PaymentConflictException("Invalid eSewa response signature");}
}
