package com.chalchitraghar.modules.notifications.specification;

import com.chalchitraghar.modules.notifications.dto.request.NotificationSearchCriteria;
import com.chalchitraghar.modules.notifications.entity.Notification;
import com.chalchitraghar.modules.notifications.enums.NotificationChannel;
import java.util.ArrayList;
import org.springframework.data.jpa.domain.Specification;

public final class NotificationSpecification {
    private NotificationSpecification() {}

    public static Specification<Notification> customer(
            Long userId, NotificationSearchCriteria criteria) {
        return (root, query, builder) -> {
            var predicates = new ArrayList<jakarta.persistence.criteria.Predicate>();
            predicates.add(builder.equal(root.get("user").get("id"), userId));
            NotificationChannel channel =
                    criteria.channel() == null ? NotificationChannel.IN_APP : criteria.channel();
            predicates.add(builder.equal(root.get("channel"), channel));
            if (criteria.type() != null)
                predicates.add(builder.equal(root.get("type"), criteria.type()));
            if (criteria.read() != null)
                predicates.add(builder.equal(root.get("read"), criteria.read()));
            if (criteria.occurredFrom() != null)
                predicates.add(
                        builder.greaterThanOrEqualTo(
                                root.get("occurredAt"), criteria.occurredFrom()));
            if (criteria.occurredTo() != null)
                predicates.add(
                        builder.lessThanOrEqualTo(root.get("occurredAt"), criteria.occurredTo()));
            return builder.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
