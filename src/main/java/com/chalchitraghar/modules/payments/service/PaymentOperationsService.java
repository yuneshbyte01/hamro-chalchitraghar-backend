package com.chalchitraghar.modules.payments.service;

import com.chalchitraghar.modules.payments.dto.response.*;
import com.chalchitraghar.modules.payments.enums.*;
import com.chalchitraghar.modules.payments.mapper.PaymentMapper;
import com.chalchitraghar.modules.payments.repository.PaymentRepository;
import com.chalchitraghar.shared.exception.*;
import com.chalchitraghar.shared.response.PageResponse;
import java.math.BigDecimal;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentOperationsService {
    private final PaymentRepository repo;
    private final PaymentMapper mapper;

    public PageResponse<AdminPaymentSummaryResponse> list(
            int page,
            int size,
            String sortBy,
            String sortDir,
            PaymentProvider provider,
            PaymentStatus status,
            String search) {
        if (page < 0 || size < 1 || size > 200)
            throw new IllegalArgumentException("Invalid pagination");
        Set<String> allowed =
                Set.of("createdAt", "updatedAt", "amount", "status", "verificationTime");
        if (!allowed.contains(sortBy))
            throw new IllegalArgumentException("Invalid payment sort field");
        var dir = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        var spec =
                (org.springframework.data.jpa.domain.Specification<
                                com.chalchitraghar.modules.payments.entity.Payment>)
                        (root, q, cb) -> {
                            var ps = new ArrayList<jakarta.persistence.criteria.Predicate>();
                            if (provider != null) ps.add(cb.equal(root.get("provider"), provider));
                            if (status != null) ps.add(cb.equal(root.get("status"), status));
                            if (search != null && !search.isBlank()) {
                                String s = "%" + search.trim().toLowerCase() + "%";
                                ps.add(
                                        cb.or(
                                                cb.like(cb.lower(root.get("paymentReference")), s),
                                                cb.like(
                                                        cb.lower(
                                                                root.get("booking")
                                                                        .get("bookingReference")),
                                                        s),
                                                cb.like(
                                                        cb.lower(
                                                                root.get("booking")
                                                                        .get("user")
                                                                        .get("email")),
                                                        s)));
                            }
                            return cb.and(
                                    ps.toArray(jakarta.persistence.criteria.Predicate[]::new));
                        };
        Page<com.chalchitraghar.modules.payments.entity.Payment> result =
                repo.findAll(spec, PageRequest.of(page, size, Sort.by(dir, sortBy)));
        return PageResponse.from(result, result.stream().map(mapper::toAdminSummary).toList());
    }

    public PageResponse<AdminPaymentSummaryResponse> manualReview(int page, int size) {
        var p =
                repo.findByManualReviewRequiredTrue(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt")));
        return PageResponse.from(p, p.stream().map(mapper::toAdminSummary).toList());
    }

    @Transactional
    public AdminPaymentDetailResponse resolve(String ref, ManualReviewResolution resolution) {
        var p =
                repo.findByPaymentReferenceForUpdate(ref.toUpperCase())
                        .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
        if (resolution != ManualReviewResolution.KEEP) {
            p.setManualReviewRequired(false);
            p.setManualReviewReason(
                    resolution == ManualReviewResolution.NO_REFUND_REQUIRED
                            ? "Resolved: no refund required"
                            : null);
        }
        return mapper.toAdminDetail(repo.save(p));
    }

    public PaymentStatisticsResponse statistics() {
        long total = repo.count(),
                success = repo.countByStatus(PaymentStatus.SUCCESS),
                pending = repo.countByStatus(PaymentStatus.PENDING),
                failed = repo.countByStatus(PaymentStatus.FAILED),
                expired = repo.countByStatus(PaymentStatus.EXPIRED);
        Object[] row = repo.successfulAmountStatistics().getFirst();
        BigDecimal revenue = decimal(row[0]), average = decimal(row[1]);
        return new PaymentStatisticsResponse(
                total,
                success,
                pending,
                failed,
                expired,
                repo.countByManualReviewRequiredTrue(),
                total == 0 ? 0 : (100.0 * success / total),
                average,
                revenue);
    }

    private BigDecimal decimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    public List<PaymentConsistencyIssue> consistency() {
        List<PaymentConsistencyIssue> out = new ArrayList<>();
        for (var p : repo.findAll()) {
            var b = p.getBooking();
            if (p.getStatus() == PaymentStatus.SUCCESS
                    && b.getStatus()
                            == com.chalchitraghar.modules.bookings.enums.BookingStatus.INITIATED)
                out.add(
                        new PaymentConsistencyIssue(
                                "SUCCESS_WITH_INITIATED_BOOKING",
                                p.getPaymentReference(),
                                b.getBookingReference(),
                                "Successful payment has an initiated booking"));
            if (p.getAmount().compareTo(b.getTotalAmount()) != 0)
                out.add(
                        new PaymentConsistencyIssue(
                                "AMOUNT_MISMATCH",
                                p.getPaymentReference(),
                                b.getBookingReference(),
                                "Payment and booking totals differ"));
        }
        return out;
    }
}
