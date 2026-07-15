package com.chalchitraghar.modules.payments.service.impl;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
import com.chalchitraghar.modules.notifications.event.*;
import com.chalchitraghar.modules.payments.config.RefundProcessingProperties;
import com.chalchitraghar.modules.payments.dto.request.*;
import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.entity.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.mapper.RefundMapper;
import com.chalchitraghar.modules.payments.repository.*;
import com.chalchitraghar.modules.payments.service.*;
import com.chalchitraghar.modules.payments.specification.RefundSpecification;
import com.chalchitraghar.modules.tickets.repository.TicketRepository;
import com.chalchitraghar.modules.users.entity.User;
import com.chalchitraghar.modules.users.repository.UserRepository;
import com.chalchitraghar.shared.exception.*;
import com.chalchitraghar.shared.response.PageResponse;
import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RefundServiceImpl implements RefundService {
    private final RefundRepository refunds;
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final UserRepository users;
    private final TicketRepository tickets;
    private final RefundReferenceGenerator references;
    private final RefundBalanceService balances;
    private final RefundEligibilityService eligibility;
    private final RefundMapper mapper;
    private final Clock clock;
    private final ApplicationEventPublisher events;
    private final RefundProcessingProperties processingProperties;

    /**
     * Lock order is Payment then Booking. No external operation is performed in this transaction.
     */
    @Override
    @Transactional
    public Refund createRefundIntent(CreateRefundIntentCommand command) {
        validateCommand(command);
        Refund existing =
                refunds.findByIdempotencyKey(command.idempotencyKey().trim()).orElse(null);
        if (existing != null) return requireSameIntent(existing, command);

        String paymentReference = normalize(command.paymentReference());
        Payment payment =
                payments.findByPaymentReferenceForUpdate(paymentReference)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Payment not found with reference: "
                                                        + paymentReference));
        existing = refunds.findByIdempotencyKey(command.idempotencyKey().trim()).orElse(null);
        if (existing != null) return requireSameIntent(existing, command);

        Booking booking =
                bookings.findByIdForUpdate(payment.getBooking().getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Booking", payment.getBooking().getId()));
        eligibility.validate(
                payment, booking, command.reason(), RefundType.FULL, RefundMethod.MANUAL);
        BigDecimal balance = balances.refundableBalance(payment);
        if (balance.compareTo(payment.getAmount()) < 0)
            throw new PaymentConflictException(
                    "Payment does not have enough refundable balance for a full refund");

        User requestedBy =
                command.requestedByUserId() == null
                        ? null
                        : users.findById(command.requestedByUserId())
                                .orElseThrow(
                                        () ->
                                                new ResourceNotFoundException(
                                                        "User", command.requestedByUserId()));
        LocalDateTime now = LocalDateTime.now(clock);
        Refund refund =
                Refund.builder()
                        .payment(payment)
                        .booking(booking)
                        .refundReference(references.generate())
                        .amount(payment.getAmount())
                        .currency(payment.getCurrency())
                        .status(RefundStatus.REQUESTED)
                        .reason(command.reason())
                        .type(RefundType.FULL)
                        .method(RefundMethod.MANUAL)
                        .idempotencyKey(command.idempotencyKey().trim())
                        .requestedBy(requestedBy)
                        .requestedAt(now)
                        .provider(payment.getProvider())
                        .maxAttempts(processingProperties.getMaxAttempts())
                        .build();
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        Refund saved = refunds.saveAndFlush(refund);
        publishRequested(saved, command.requestedByUserId(), now);
        return saved;
    }

    @Override
    @Transactional
    public AdminRefundDetailResponse createAdminRefund(
            AdminCreateRefundRequest request, String idempotencyKey, User admin) {
        if (!java.util.EnumSet.of(
                        RefundReason.ADMIN_ADJUSTMENT,
                        RefundReason.LATE_PAYMENT_SUCCESS,
                        RefundReason.DUPLICATE_PAYMENT,
                        RefundReason.SHOW_CANCELLATION)
                .contains(request.reason())) {
            throw new IllegalArgumentException("Refund reason is not available for admin creation");
        }
        Refund refund =
                createRefundIntent(
                        new CreateRefundIntentCommand(
                                request.paymentReference(),
                                request.reason(),
                                idempotencyKey,
                                admin.getId()));
        return mapper.toAdminDetail(
                refund, tickets.findByBookingIdOrderByIssuedAtAsc(refund.getBooking().getId()));
    }

    @Override
    @Transactional
    public AdminRefundDetailResponse approveRefund(String reference, User admin) {
        Refund refund = lockRefund(reference);
        if (refund.getStatus() == RefundStatus.APPROVED) return detail(refund);
        if (refund.getStatus() != RefundStatus.REQUESTED)
            throw new PaymentConflictException("Only requested refunds can be approved");
        Payment payment =
                payments.findByPaymentReferenceForUpdate(refund.getPayment().getPaymentReference())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        Booking booking =
                bookings.findByIdForUpdate(refund.getBooking().getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Booking", refund.getBooking().getId()));
        eligibility.validate(
                payment, booking, refund.getReason(), refund.getType(), refund.getMethod());
        if (refund.getAmount().compareTo(payment.getAmount()) != 0
                || !refund.getCurrency().equals(payment.getCurrency())
                || RefundBalanceService.RESERVING_STATUSES.contains(refund.getStatus())
                        && balances.refundableBalance(payment).signum() != 0) {
            throw new PaymentConflictException(
                    "Refund no longer matches the reserved payment balance");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        refund.setStatus(RefundStatus.APPROVED);
        refund.setApprovedBy(admin);
        refund.setApprovedAt(now);
        refund.setUpdatedAt(now);
        Refund saved = refunds.saveAndFlush(refund);
        publishApproved(saved, admin == null ? null : admin.getId(), now);
        return detail(saved);
    }

    @Override
    @Transactional
    public AdminRefundDetailResponse rejectRefund(
            String reference, AdminRejectRefundRequest request, User admin) {
        Refund refund = lockRefund(reference);
        if (refund.getStatus() == RefundStatus.REJECTED) return detail(refund);
        if (refund.getStatus() != RefundStatus.REQUESTED)
            throw new PaymentConflictException("Only requested refunds can be rejected");
        LocalDateTime now = LocalDateTime.now(clock);
        refund.setStatus(RefundStatus.REJECTED);
        refund.setRejectedBy(admin);
        refund.setRejectedAt(now);
        refund.setRejectionReasonCode(normalizeCode(request.reasonCode()));
        refund.setRejectionNote(sanitizeNote(request.note()));
        refund.setUpdatedAt(now);
        Refund saved = refunds.saveAndFlush(refund);
        publishRejected(saved, admin.getId(), now);
        return detail(saved);
    }

    @Override
    public PageResponse<CustomerRefundSummaryResponse> getCustomerRefunds(
            CustomerRefundFilter filter, int page, int size, User user) {
        validatePageAndDates(page, size, filter.requestedFrom(), filter.requestedTo());
        Page<Refund> result =
                refunds.findAll(
                        RefundSpecification.customer(filter, user.getId()), pageable(page, size));
        return PageResponse.from(result, result.stream().map(mapper::toCustomerSummary).toList());
    }

    @Override
    public CustomerRefundDetailResponse getCustomerRefund(String reference, User user) {
        return mapper.toCustomerDetail(
                refunds.findByRefundReferenceAndBookingUserId(normalize(reference), user.getId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Refund not found with reference: " + reference)));
    }

    @Override
    public List<CustomerRefundSummaryResponse> getCustomerBookingRefunds(
            String reference, User user) {
        Booking booking =
                bookings.findByBookingReference(normalize(reference))
                        .filter(b -> b.getUser().getId().equals(user.getId()))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Booking not found with reference: " + reference));
        return refunds.findByBookingIdOrderByRequestedAtDescIdDesc(booking.getId()).stream()
                .map(mapper::toCustomerSummary)
                .toList();
    }

    @Override
    public PageResponse<AdminRefundSummaryResponse> getAdminRefunds(
            AdminRefundFilter filter, int page, int size) {
        validateAdmin(filter, page, size);
        Page<Refund> result =
                refunds.findAll(RefundSpecification.admin(filter), pageable(page, size));
        return PageResponse.from(result, result.stream().map(mapper::toAdminSummary).toList());
    }

    @Override
    public AdminRefundDetailResponse getAdminRefund(String reference) {
        Refund refund =
                refunds.findByRefundReference(normalize(reference))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Refund not found with reference: " + reference));
        return mapper.toAdminDetail(
                refund, tickets.findByBookingIdOrderByIssuedAtAsc(refund.getBooking().getId()));
    }

    private void validateCommand(CreateRefundIntentCommand command) {
        if (command == null || command.reason() == null)
            throw new IllegalArgumentException("Refund reason is required");
        if (command.paymentReference() == null || command.paymentReference().isBlank())
            throw new IllegalArgumentException("Payment reference is required");
        if (command.idempotencyKey() == null || command.idempotencyKey().isBlank())
            throw new IllegalArgumentException("Refund idempotency key is required");
        if (command.idempotencyKey().trim().length() > 200)
            throw new IllegalArgumentException(
                    "Refund idempotency key must not exceed 200 characters");
    }

    private Refund requireSameIntent(Refund existing, CreateRefundIntentCommand command) {
        if (!existing.getPayment()
                        .getPaymentReference()
                        .equals(normalize(command.paymentReference()))
                || existing.getReason() != command.reason()) {
            throw new PaymentConflictException(
                    "Refund idempotency key was already used with different intent parameters");
        }
        return existing;
    }

    private Refund lockRefund(String reference) {
        return refunds.findByRefundReferenceForUpdate(normalize(reference))
                .orElseThrow(
                        () ->
                                new ResourceNotFoundException(
                                        "Refund not found with reference: " + reference));
    }

    private AdminRefundDetailResponse detail(Refund refund) {
        return mapper.toAdminDetail(
                refund, tickets.findByBookingIdOrderByIssuedAtAsc(refund.getBooking().getId()));
    }

    private String normalizeCode(String value) {
        String code = value == null ? "" : value.trim().toUpperCase();
        if (!code.matches("[A-Z0-9_]{1,50}"))
            throw new IllegalArgumentException("Invalid rejection reason code");
        return code;
    }

    private String sanitizeNote(String value) {
        if (value == null || value.isBlank()) return null;
        String safe = value.replaceAll("[\\p{Cntrl}&&[^\\r\\n\\t]]", "").trim();
        if (safe.length() > 500)
            throw new IllegalArgumentException("Rejection note must not exceed 500 characters");
        return safe;
    }

    private void publishRequested(Refund r, Long actorId, LocalDateTime at) {
        events.publishEvent(
                new RefundRequestedEvent(
                        r.getId(),
                        r.getRefundReference(),
                        r.getBooking().getUser().getId(),
                        actorId,
                        r.getBooking().getBookingReference(),
                        r.getPayment().getPaymentReference(),
                        r.getAmount(),
                        r.getCurrency(),
                        r.getReason(),
                        r.getStatus(),
                        at));
    }

    private void publishApproved(Refund r, Long actorId, LocalDateTime at) {
        events.publishEvent(
                new RefundApprovedEvent(
                        r.getId(),
                        r.getRefundReference(),
                        r.getBooking().getUser().getId(),
                        actorId,
                        r.getBooking().getBookingReference(),
                        r.getPayment().getPaymentReference(),
                        r.getAmount(),
                        r.getCurrency(),
                        r.getReason(),
                        r.getStatus(),
                        at));
    }

    private void publishRejected(Refund r, Long actorId, LocalDateTime at) {
        events.publishEvent(
                new RefundRejectedEvent(
                        r.getId(),
                        r.getRefundReference(),
                        r.getBooking().getUser().getId(),
                        actorId,
                        r.getBooking().getBookingReference(),
                        r.getPayment().getPaymentReference(),
                        r.getAmount(),
                        r.getCurrency(),
                        r.getReason(),
                        r.getStatus(),
                        at));
    }

    private void validateAdmin(AdminRefundFilter f, int page, int size) {
        validatePageAndDates(page, size, f.requestedFrom(), f.requestedTo());
        if (f.amountFrom() != null && f.amountFrom().signum() < 0
                || f.amountTo() != null && f.amountTo().signum() < 0)
            throw new IllegalArgumentException("Refund amount filters must not be negative");
        if (f.amountFrom() != null
                && f.amountTo() != null
                && f.amountFrom().compareTo(f.amountTo()) > 0)
            throw new IllegalArgumentException("amountFrom must not be after amountTo");
    }

    private void validatePageAndDates(int page, int size, LocalDateTime from, LocalDateTime to) {
        if (page < 0 || size < 1 || size > 200)
            throw new IllegalArgumentException("Invalid pagination");
        if (from != null && to != null && from.isAfter(to))
            throw new IllegalArgumentException("requestedFrom must not be after requestedTo");
    }

    private Pageable pageable(int page, int size) {
        return PageRequest.of(
                page, size, Sort.by(Sort.Order.desc("requestedAt"), Sort.Order.desc("id")));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }
}
