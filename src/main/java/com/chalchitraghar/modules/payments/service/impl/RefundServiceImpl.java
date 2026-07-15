package com.chalchitraghar.modules.payments.service.impl;

import com.chalchitraghar.modules.bookings.entity.Booking;
import com.chalchitraghar.modules.bookings.repository.BookingRepository;
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
                        .build();
        refund.setCreatedAt(now);
        refund.setUpdatedAt(now);
        return refunds.saveAndFlush(refund);
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
