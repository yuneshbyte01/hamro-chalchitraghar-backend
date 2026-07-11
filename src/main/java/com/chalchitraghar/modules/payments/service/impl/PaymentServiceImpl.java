package com.chalchitraghar.modules.payments.service.impl;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.entity.Payment;
import com.chalchitraghar.modules.payments.mapper.PaymentMapper;
import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.modules.payments.service.PaymentService;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.shared.exception.ResourceNotFoundException;
import com.chalchitraghar.shared.exception.PaymentConflictException;
import com.chalchitraghar.modules.payments.dto.request.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.provider.PaymentProviderAdapter;
import com.chalchitraghar.modules.payments.service.*;
import com.chalchitraghar.modules.bookings.enums.BookingStatus;
import com.chalchitraghar.modules.shows.service.ShowLifecycleService;
import java.time.*;
import org.springframework.beans.factory.annotation.Value;
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper mapper;
    private final PaymentReferenceGenerator referenceGenerator;
    private final PaymentLifecycleService lifecycle;
    private final ShowLifecycleService showLifecycleService;
    private final List<PaymentProviderAdapter> providerAdapters;
    private final Clock clock;
    @Value("${app.payments.attempt-expiration-minutes:10}") private long expirationMinutes;

    public List<CustomerPaymentSummaryResponse> getMyPayments(User user) {
        return paymentRepository.findByBookingUserIdOrderByCreatedAtDesc(user.getId()).stream().map(mapper::toCustomerSummary).toList();
    }
    public CustomerPaymentDetailResponse getMyPaymentByReference(String reference, User user) {
        Payment payment = findPayment(reference);
        if (!payment.getBooking().getUser().getId().equals(user.getId())) throw paymentNotFound(reference);
        return mapper.toCustomerDetail(payment);
    }
    public List<CustomerPaymentSummaryResponse> getMyPaymentsByBookingReference(String reference, User user) {
        Booking booking = findBooking(reference);
        if (!booking.getUser().getId().equals(user.getId())) throw bookingNotFound(reference);
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream().map(mapper::toCustomerSummary).toList();
    }
    public StaffPaymentDetailResponse getStaffPaymentByReference(String reference) { return mapper.toStaffDetail(findPayment(reference)); }
    public List<StaffPaymentSummaryResponse> getStaffPaymentsByBookingReference(String reference) {
        Booking booking=findBooking(reference); return paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream().map(mapper::toStaffSummary).toList();
    }
    public AdminPaymentDetailResponse getAdminPaymentByReference(String reference) { return mapper.toAdminDetail(findPayment(reference)); }
    public List<AdminPaymentSummaryResponse> getAdminPaymentsByBookingReference(String reference) {
        Booking booking=findBooking(reference); return paymentRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId()).stream().map(mapper::toAdminSummary).toList();
    }
    @Transactional
    public CustomerPaymentDetailResponse initiate(String reference, String rawKey, PaymentInitiationRequest request, User user) {
        String key=validateKey(rawKey);
        Booking booking=bookingRepository.findByIdForUpdate(findBooking(reference).getId()).orElseThrow(() -> bookingNotFound(reference));
        if (!booking.getUser().getId().equals(user.getId())) throw bookingNotFound(reference);
        Payment prior=paymentRepository.findByBookingIdAndIdempotencyKey(booking.getId(),key).orElse(null);
        if (prior!=null) {
            if (prior.getProvider()==request.provider() && prior.getMethod()==request.method()) return mapper.toCustomerDetail(prior);
            throw new PaymentConflictException("Idempotency key was already used with different payment parameters");
        }
        if (booking.getStatus()!=BookingStatus.INITIATED) throw new PaymentConflictException("Only INITIATED bookings can start payment");
        if (booking.getExpiresAt()!=null && !LocalDateTime.now(clock).isBefore(booking.getExpiresAt())) throw new PaymentConflictException("Booking has expired and cannot start payment");
        showLifecycleService.assertBookable(booking.getShow());
        var active=paymentRepository.findByBookingIdAndStatusIn(booking.getId(),PaymentLifecycleService.ACTIVE);
        if (!active.isEmpty()) throw new PaymentConflictException("Booking already has an active payment attempt");
        if (paymentRepository.existsByBookingIdAndStatus(booking.getId(),PaymentStatus.SUCCESS)) throw new PaymentConflictException("Booking already has a successful payment");
        PaymentProviderAdapter adapter=providerAdapters.stream().filter(a->a.supports(request.provider())).findFirst()
                .orElseThrow(()->new PaymentConflictException("Payment provider is not available"));
        if (request.method()!=PaymentMethod.ONLINE) throw new PaymentConflictException("Payment method is not available");
        LocalDateTime now=LocalDateTime.now(clock);
        Payment payment=Payment.builder().booking(booking).paymentReference(referenceGenerator.generate()).provider(request.provider())
                .method(request.method()).status(PaymentStatus.CREATED).amount(booking.getTotalAmount()).currency(booking.getCurrency())
                .idempotencyKey(key).initiatedAt(now).expiresAt(now.plusMinutes(expirationMinutes)).build();
        adapter.initiate(payment);
        return mapper.toCustomerDetail(paymentRepository.save(payment));
    }
    @Transactional
    public CustomerPaymentDetailResponse cancel(String reference, User user) {
        Payment p=findPaymentForUpdate(reference,user); lifecycle.reconcileExpiry(p);
        if (p.getStatus()==PaymentStatus.CANCELLED) return mapper.toCustomerDetail(p);
        lifecycle.transition(p,PaymentStatus.CANCELLED); return mapper.toCustomerDetail(paymentRepository.save(p));
    }
    @Transactional(noRollbackFor = PaymentConflictException.class)
    public CustomerPaymentDetailResponse processLocal(String reference, LocalPaymentProcessRequest request, User user) {
        Payment p=findPaymentForUpdate(reference,user); lifecycle.reconcileExpiry(p);
        if (p.getProvider()!=PaymentProvider.LOCAL) throw new PaymentConflictException("Only LOCAL payments can be processed locally");
        PaymentStatus target=request.result()==LocalPaymentProcessRequest.LocalPaymentResult.SUCCESS?PaymentStatus.SUCCESS:PaymentStatus.FAILED;
        lifecycle.transition(p,target);
        if(target==PaymentStatus.FAILED){p.setFailureCode("LOCAL_FAILED");p.setFailureMessage("Local payment was marked as failed");}
        return mapper.toCustomerDetail(paymentRepository.save(p));
    }
    private Payment findPaymentForUpdate(String ref,User user){Payment p=paymentRepository.findByPaymentReferenceForUpdate(normalize(ref)).orElseThrow(()->paymentNotFound(ref)); if(!p.getBooking().getUser().getId().equals(user.getId()))throw paymentNotFound(ref); return p;}
    private String validateKey(String raw){if(raw==null||raw.trim().isEmpty())throw new IllegalArgumentException("Idempotency-Key must not be blank");String key=raw.trim();if(key.length()>255)throw new IllegalArgumentException("Idempotency-Key must not exceed 255 characters");return key;}
    private Payment findPayment(String ref) { return paymentRepository.findByPaymentReference(normalize(ref)).orElseThrow(() -> paymentNotFound(ref)); }
    private Booking findBooking(String ref) { return bookingRepository.findByBookingReference(normalize(ref)).orElseThrow(() -> bookingNotFound(ref)); }
    private String normalize(String ref) { return ref == null ? "" : ref.trim().toUpperCase(); }
    private ResourceNotFoundException paymentNotFound(String ref) { return new ResourceNotFoundException("Payment not found with reference: " + ref); }
    private ResourceNotFoundException bookingNotFound(String ref) { return new ResourceNotFoundException("Booking not found with reference: " + ref); }
}
