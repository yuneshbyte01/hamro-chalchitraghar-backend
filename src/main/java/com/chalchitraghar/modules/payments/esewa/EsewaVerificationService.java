package com.chalchitraghar.modules.payments.esewa;

import com.chalchitraghar.modules.payments.config.EsewaProperties;
import com.chalchitraghar.modules.payments.dto.esewa.*;
import com.chalchitraghar.modules.payments.dto.response.CustomerPaymentDetailResponse;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.enums.PaymentProvider;
import com.chalchitraghar.modules.payments.mapper.PaymentMapper;
import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.shared.exception.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class EsewaVerificationService {
    private static final List<String> FIELDS =
            List.of(
                    "transaction_code",
                    "status",
                    "total_amount",
                    "transaction_uuid",
                    "product_code",
                    "signed_field_names");
    private final ObjectMapper json;
    private final EsewaProperties p;
    private final EsewaSignatureService signatures;
    private final EsewaStatusClient client;
    private final EsewaPaymentFinalizer finalizer;
    private final PaymentRepository payments;
    private final PaymentMapper mapper;

    public EsewaVerificationService(
            ObjectMapper j,
            EsewaProperties p,
            EsewaSignatureService s,
            EsewaStatusClient c,
            EsewaPaymentFinalizer f,
            PaymentRepository r,
            PaymentMapper m) {
        json = j;
        this.p = p;
        signatures = s;
        client = c;
        finalizer = f;
        payments = r;
        mapper = m;
    }

    public CustomerPaymentDetailResponse verify(String data) {
        EsewaDecodedResponse d = decode(data);
        Payment payment = validate(d);
        EsewaStatusResponse status = client.check(payment);
        validateStatus(payment, d, status);
        return mapper.toCustomerDetail(
                finalizer.finalizeStatus(
                        payment.getPaymentReference(), status, d.transactionCode()));
    }

    public Payment reconcile(String reference) {
        Payment payment =
                payments.findByPaymentReference(reference.toUpperCase())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Payment not found with reference: " + reference));
        if (payment.getProvider() != PaymentProvider.ESEWA)
            throw new PaymentConflictException("Only eSewa payments can be reconciled");
        EsewaStatusResponse s = client.check(payment);
        validateStatus(payment, null, s);
        return finalizer.finalizeStatus(payment.getPaymentReference(), s, s.refId());
    }

    private EsewaDecodedResponse decode(String data) {
        try {
            byte[] bytes = Base64.getDecoder().decode(data);
            if (bytes.length > 12288) throw new IllegalArgumentException();
            return json.readValue(
                    new String(bytes, StandardCharsets.UTF_8), EsewaDecodedResponse.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid eSewa response data");
        }
    }

    private Payment validate(EsewaDecodedResponse d) {
        if (d.transactionCode() == null
                || d.status() == null
                || d.totalAmount() == null
                || d.transactionUuid() == null
                || d.productCode() == null
                || d.signedFieldNames() == null
                || d.signature() == null)
            throw new IllegalArgumentException("eSewa response is missing required fields");
        List<String> names = Arrays.asList(d.signedFieldNames().split(",", -1));
        if (!names.equals(FIELDS) || new HashSet<>(names).size() != names.size())
            throw new PaymentConflictException("Unexpected eSewa signed fields");
        String message =
                "transaction_code="
                        + d.transactionCode()
                        + ",status="
                        + d.status()
                        + ",total_amount="
                        + d.totalAmount()
                        + ",transaction_uuid="
                        + d.transactionUuid()
                        + ",product_code="
                        + d.productCode()
                        + ",signed_field_names="
                        + d.signedFieldNames();
        signatures.verify(message, d.signature());
        Payment x =
                payments.findByPaymentReference(d.transactionUuid())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (x.getProvider() != PaymentProvider.ESEWA
                || !p.productCode().equals(d.productCode())
                || new BigDecimal(d.totalAmount().replace(",", "")).compareTo(x.getAmount()) != 0)
            throw new PaymentConflictException("eSewa response does not match payment");
        return x;
    }

    private void validateStatus(Payment x, EsewaDecodedResponse d, EsewaStatusResponse s) {
        if (s == null
                || !p.productCode().equals(s.productCode())
                || !x.getPaymentReference().equals(s.transactionUuid())
                || s.totalAmount() == null
                || s.totalAmount().compareTo(x.getAmount()) != 0)
            throw new PaymentConflictException("eSewa status response does not match payment");
        if (d != null && !d.status().equals(s.status()))
            throw new PaymentConflictException("eSewa redirect and status response disagree");
    }
}
