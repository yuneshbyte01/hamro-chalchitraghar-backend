package com.chalchitraghar.modules.tickets.specification;

import com.chalchitraghar.modules.tickets.dto.request.TicketSearchCriteria;
import com.chalchitraghar.modules.tickets.entity.Ticket;
import com.chalchitraghar.modules.tickets.enums.TicketStatus;
import org.springframework.data.jpa.domain.Specification;

public final class TicketSpecification {
    private TicketSpecification() {}

    public static Specification<Ticket> search(TicketSearchCriteria c, Long owner) {
        return (r, q, b) -> {
            var p = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
            var booking = r.get("booking");
            var show = booking.get("show");
            var seat = r.get("bookingSeat").get("seat");
            if (owner != null) p.add(b.equal(booking.get("user").get("id"), owner));
            if (c.status() != null && !c.status().isBlank())
                p.add(
                        b.equal(
                                r.get("status"),
                                TicketStatus.valueOf(c.status().trim().toUpperCase())));
            if (c.showId() != null) p.add(b.equal(show.get("id"), c.showId()));
            if (c.movieId() != null) p.add(b.equal(show.get("movie").get("id"), c.movieId()));
            if (c.hallId() != null) p.add(b.equal(show.get("hall").get("id"), c.hallId()));
            if (c.customerId() != null)
                p.add(b.equal(booking.get("user").get("id"), c.customerId()));
            if (c.bookingReference() != null && !c.bookingReference().isBlank())
                p.add(
                        b.equal(
                                booking.get("bookingReference"),
                                c.bookingReference().trim().toUpperCase()));
            if (c.showDateFrom() != null)
                p.add(b.greaterThanOrEqualTo(show.get("showDate"), c.showDateFrom()));
            if (c.showDateTo() != null)
                p.add(b.lessThanOrEqualTo(show.get("showDate"), c.showDateTo()));
            if (c.checkedInFrom() != null)
                p.add(b.greaterThanOrEqualTo(r.get("checkedInAt"), c.checkedInFrom()));
            if (c.checkedInTo() != null)
                p.add(b.lessThanOrEqualTo(r.get("checkedInAt"), c.checkedInTo()));
            if (c.issuedFrom() != null)
                p.add(b.greaterThanOrEqualTo(r.get("issuedAt"), c.issuedFrom()));
            if (c.issuedTo() != null) p.add(b.lessThanOrEqualTo(r.get("issuedAt"), c.issuedTo()));
            if (c.revokedBy() != null) p.add(b.equal(r.get("revokedBy").get("id"), c.revokedBy()));
            if (c.checkedInBy() != null)
                p.add(b.equal(r.get("checkedInBy").get("id"), c.checkedInBy()));
            if (c.search() != null && !c.search().isBlank()) {
                String s = "%" + c.search().trim().toLowerCase() + "%";
                p.add(
                        b.or(
                                b.like(b.lower(r.get("ticketReference")), s),
                                b.like(b.lower(booking.get("bookingReference")), s),
                                b.like(b.lower(booking.get("user").get("name")), s),
                                b.like(b.lower(booking.get("user").get("email")), s),
                                b.like(b.lower(show.get("movie").get("title")), s),
                                b.like(b.lower(show.get("hall").get("name")), s),
                                b.like(b.lower(seat.get("seatCode")), s)));
            }
            return b.and(p.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
