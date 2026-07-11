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
import lombok.RequiredArgsConstructor;

@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PaymentMapper mapper;

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
    private Payment findPayment(String ref) { return paymentRepository.findByPaymentReference(normalize(ref)).orElseThrow(() -> paymentNotFound(ref)); }
    private Booking findBooking(String ref) { return bookingRepository.findByBookingReference(normalize(ref)).orElseThrow(() -> bookingNotFound(ref)); }
    private String normalize(String ref) { return ref == null ? "" : ref.trim().toUpperCase(); }
    private ResourceNotFoundException paymentNotFound(String ref) { return new ResourceNotFoundException("Payment not found with reference: " + ref); }
    private ResourceNotFoundException bookingNotFound(String ref) { return new ResourceNotFoundException("Booking not found with reference: " + ref); }
}
