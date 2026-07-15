package com.chalchitraghar.modules.payments.specification;

import com.chalchitraghar.modules.payments.dto.request.*;
import com.chalchitraghar.modules.payments.entity.Refund;
import jakarta.persistence.criteria.JoinType;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

public final class RefundSpecification {
    private RefundSpecification() {}

    public static Specification<Refund> customer(CustomerRefundFilter f, Long ownerId) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(cb.equal(root.join("booking").join("user").get("id"), ownerId));
            if (f.status() != null) predicates.add(cb.equal(root.get("status"), f.status()));
            if (f.reason() != null) predicates.add(cb.equal(root.get("reason"), f.reason()));
            if (f.requestedFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), f.requestedFrom()));
            if (f.requestedTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("requestedAt"), f.requestedTo()));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    public static Specification<Refund> admin(AdminRefundFilter f) {
        return (root, query, cb) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            var payment = root.join("payment", JoinType.INNER);
            var booking = root.join("booking", JoinType.INNER);
            var user = booking.join("user", JoinType.INNER);
            if (text(f.refundReference()))
                predicates.add(cb.equal(root.get("refundReference"), upper(f.refundReference())));
            if (text(f.paymentReference()))
                predicates.add(
                        cb.equal(payment.get("paymentReference"), upper(f.paymentReference())));
            if (text(f.bookingReference()))
                predicates.add(
                        cb.equal(booking.get("bookingReference"), upper(f.bookingReference())));
            if (f.customerId() != null) predicates.add(cb.equal(user.get("id"), f.customerId()));
            if (text(f.customerEmail()))
                predicates.add(
                        cb.equal(
                                cb.lower(user.get("email")),
                                f.customerEmail().trim().toLowerCase()));
            if (f.status() != null) predicates.add(cb.equal(root.get("status"), f.status()));
            if (f.reason() != null) predicates.add(cb.equal(root.get("reason"), f.reason()));
            if (f.type() != null) predicates.add(cb.equal(root.get("type"), f.type()));
            if (f.method() != null) predicates.add(cb.equal(root.get("method"), f.method()));
            if (f.provider() != null) predicates.add(cb.equal(root.get("provider"), f.provider()));
            if (f.requestedFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("requestedAt"), f.requestedFrom()));
            if (f.requestedTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("requestedAt"), f.requestedTo()));
            if (f.amountFrom() != null)
                predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), f.amountFrom()));
            if (f.amountTo() != null)
                predicates.add(cb.lessThanOrEqualTo(root.get("amount"), f.amountTo()));
            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private static boolean text(String value) {
        return value != null && !value.isBlank();
    }

    private static String upper(String value) {
        return value.trim().toUpperCase();
    }
}
